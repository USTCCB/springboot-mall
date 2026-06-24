package com.ustccb.mall.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 商品服务：Cache Aside 缓存模式
 * <pre>
 *   读：Redis MISS -> 查 DB -> 写 Redis (TTL=5min)
 *   写（更新/删除）：先写 DB，再 DEL Redis（不让缓存脏读）
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

    public List<Goods> list() {
        return goodsMapper.findAll();
    }

    /**
     * 查询商品详情：Cache Aside 读路径
     */
    public Goods get(Long id) {
        String key = CACHE_KEY_PREFIX + id;
        // 1) 读缓存
        String json = redis.opsForValue().get(key);
        if (json != null) {
            try {
                log.debug("商品缓存命中 id={}", id);
                return objectMapper.readValue(json, Goods.class);
            } catch (Exception e) {
                log.warn("反序列化缓存失败，降级到 DB: {}", e.getMessage());
            }
        }
        // 2) MISS -> 查 DB
        Goods g = goodsMapper.findById(id);
        if (g != null) {
            // 3) 写回缓存
            try {
                redis.opsForValue().set(key, objectMapper.writeValueAsString(g), CACHE_TTL);
            } catch (Exception e) {
                log.warn("写入缓存失败: {}", e.getMessage());
            }
        }
        return g;
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
