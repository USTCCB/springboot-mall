package com.ustccb.mall.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 商品服务：Cache Aside 缓存模式
 * <pre>
 *   - 读防线：Redis 命中 -> 返回；MISS -> 抢分布式锁（防击穿） -> 二重检查缓存 -> 查 DB -> 正常写缓存/存空标记（防穿透） -> 释锁
 *   - 写防线：先写 DB，再 DEL Redis，保证强一致性
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService {

    private static final String CACHE_KEY_PREFIX = "mall:goods:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final GoodsMapper goodsMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RedissonClient redisson;

    public List<Goods> list() {
        return goodsMapper.findAll();
    }

    /**
     * 查询商品详情：带高防卫能力的 Cache Aside 读路径
     */
    public Goods get(Long id) {
        String key = CACHE_KEY_PREFIX + id;
        
        // 1) 读缓存：快速通道
        String json = redis.opsForValue().get(key);
        if (json != null) {
            if ("{}".equals(json)) {
                log.debug("商品缓存命中空值(防缓存穿透) id={}", id);
                return null;
            }
            try {
                log.debug("商品缓存命中 id={}", id);
                return objectMapper.readValue(json, Goods.class);
            } catch (Exception e) {
                log.warn("反序列化缓存失败，降级到 DB: {}", e.getMessage());
            }
        }

        // 2) 缓存失效：获取互斥锁防缓存击穿
        String lockKey = CACHE_KEY_PREFIX + "lock:" + id;
        RLock lock = redisson.getLock(lockKey);
        try {
            // 尝试获取锁，等待时间 2 秒，占锁 5 秒
            boolean locked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (locked) {
                try {
                    // Double Check (二重检查)：获取到锁之后，再次查缓存，防止等待时别的线程已经将数据写入缓存
                    json = redis.opsForValue().get(key);
                    if (json != null) {
                        if ("{}".equals(json)) {
                            return null;
                        }
                        return objectMapper.readValue(json, Goods.class);
                    }

                    // 查 DB
                    Goods g = goodsMapper.findById(id);
                    if (g != null) {
                        // 查到数据：回写缓存，设置 5 分钟 TTL
                        redis.opsForValue().set(key, objectMapper.writeValueAsString(g), CACHE_TTL);
                    } else {
                        // DB 无此数据：缓存空对象（如 "{}"），TTL设为 60 秒，有效防缓存穿透
                        redis.opsForValue().set(key, "{}", Duration.ofSeconds(60));
                    }
                    return g;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // 未抢到锁的线程进行休眠重试，防止将压力全部打进 DB
                log.debug("未获取到防击穿锁，休眠后重试 id={}", id);
                Thread.sleep(100);
                return get(id); // 递归重试，此时缓存已被抢到锁的线程填充完毕，能直接命中快速通道
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("防击穿获取锁被中断，直接降级查 DB id={}", id);
            return goodsMapper.findById(id);
        } catch (Exception e) {
            log.error("防击穿查询出现异常，降级直连 DB id={}", id, e);
            return goodsMapper.findById(id);
        }
    }

    /**
     * 更新商品：先写 DB，再清缓存（Cache Aside 写路径）
     */
    @Transactional
    public Goods update(Long id, Goods g) {
        g.setId(id);
        goodsMapper.update(g);
        // DEL 而非 SET，避免并发写覆盖
        redis.delete(CACHE_KEY_PREFIX + id);
        log.info("更新商品并清缓存 id={}", id);
        return goodsMapper.findById(id);
    }
}

