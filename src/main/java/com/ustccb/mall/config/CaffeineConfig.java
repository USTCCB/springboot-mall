package com.ustccb.mall.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ustccb.mall.entity.Goods;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地高性能缓存配置 (一级缓存)
 */
@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<Long, Goods> goodsLocalCache() {
        return Caffeine.newBuilder()
                // 初始缓存容量
                .initialCapacity(100)
                // 最大缓存条数，超过此容量后将基于 LRU 策略剔除
                .maximumSize(1000)
                // 写入后过期时间设定为 1 分钟 (轻量级本地缓存策略，防过长脏数据)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();
    }
}
