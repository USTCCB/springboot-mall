package com.ustccb.mall.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.ustccb.mall.annotation.ReadOnly;
import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务：多级缓存 (Caffeine 一级 + Redis 二级) + 读写分离架构
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService {

    private static final String CACHE_KEY_PREFIX = "mall:goods:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // 本地多级缓存穿透防护空标记对象
    private static final Goods EMPTY_GOODS = new Goods();
    static {
        EMPTY_GOODS.setId(-1L);
    }

    private final GoodsMapper goodsMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RedissonClient redisson;
    private final Cache<Long, Goods> goodsLocalCache; // 注入一级的 Caffeine 本地缓存

    /**
     * 商品列表：查询操作路由至只读从数据库 (slave)
     */
    @ReadOnly
    public List<Goods> list() {
        return goodsMapper.findAll();
    }

    /**
     * 查询商品详情：多级缓存级联读取通道
     */
    @ReadOnly // 读操作路由到只读从库
    public Goods get(Long id) {
        String key = CACHE_KEY_PREFIX + id;

        // 1) 读一级本地缓存 (Caffeine)：极速响应，抗极高吞吐量
        Goods localGoods = goodsLocalCache.getIfPresent(id);
        if (localGoods != null) {
            if (EMPTY_GOODS.getId().equals(localGoods.getId())) {
                log.debug("[Caffeine] 命中本地空值防护，直接返回 null. id={}", id);
                return null;
            }
            log.debug("[Caffeine] 一级本地缓存命中. id={}", id);
            return localGoods;
        }

        // 2) 读二级分布式缓存 (Redis)：分布式共享
        String json = redis.opsForValue().get(key);
        if (json != null) {
            if ("{}".equals(json)) {
                log.debug("[Redis] 二级缓存命中空值，填充一级本地并返回 null. id={}", id);
                goodsLocalCache.put(id, EMPTY_GOODS);
                return null;
            }
            try {
                Goods g = objectMapper.readValue(json, Goods.class);
                log.debug("[Redis] 二级分布式缓存命中. id={}", id);
                // 填充一级缓存，保证下次请求直接走 Caffeine
                goodsLocalCache.put(id, g);
                return g;
            } catch (Exception e) {
                log.warn("二级缓存解析失败，降级处理: {}", e.getMessage());
            }
        }

        // 3) 缓存击穿防护：获取分布式互斥锁并执行 DCL（双重校验锁）
        String lockKey = CACHE_KEY_PREFIX + "lock:" + id;
        RLock lock = redisson.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (locked) {
                try {
                    // Double Check：获取锁后再次检查本地和 Redis 缓存，防高并发下排队等待释放后重复查询 DB
                    localGoods = goodsLocalCache.getIfPresent(id);
                    if (localGoods != null) {
                        return EMPTY_GOODS.getId().equals(localGoods.getId()) ? null : localGoods;
                    }
                    json = redis.opsForValue().get(key);
                    if (json != null) {
                        if ("{}".equals(json)) {
                            goodsLocalCache.put(id, EMPTY_GOODS);
                            return null;
                        }
                        Goods g = objectMapper.readValue(json, Goods.class);
                        goodsLocalCache.put(id, g);
                        return g;
                    }

                    // 4) 最终路由从库查询 DB
                    Goods g = goodsMapper.findById(id);
                    if (g != null) {
                        log.info("[DB] 缓存未命中，查询从库成功并写回多级缓存. id={}", id);
                        redis.opsForValue().set(key, objectMapper.writeValueAsString(g), CACHE_TTL);
                        goodsLocalCache.put(id, g);
                    } else {
                        log.warn("[DB] 查询从库发现商品不存在，回写空标记防穿透. id={}", id);
                        redis.opsForValue().set(key, "{}", Duration.ofSeconds(60));
                        goodsLocalCache.put(id, EMPTY_GOODS);
                    }
                    return g;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // 没抢到锁的线程休眠后重试，此时获取到锁的线程应该已经写好了缓存
                Thread.sleep(100);
                return get(id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return goodsMapper.findById(id);
        } catch (Exception e) {
            log.error("多级缓存击穿防护交易异常，降级直连从库 DB. id={}", id, e);
            return goodsMapper.findById(id);
        }
    }

    /**
     * 更新商品：写操作直接路由到主数据库 (master)
     */
    @Transactional
    public Goods update(Long id, Goods g) {
        g.setId(id);
        goodsMapper.update(g);
        // 双删/单删：Cache Aside 写路径，失效缓存，保持强一致
        redis.delete(CACHE_KEY_PREFIX + id);
        goodsLocalCache.invalidate(id); // 同时使一级本地缓存失效
        log.info("[DB] 更新主库并使多级缓存失效 id={}", id);
        return goodsMapper.findById(id);
    }
}
