package com.ustccb.mall.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis 相关 Bean 配置
 */
@Configuration
public class RedisConfig {

    /** 用于 Cache Aside 中 Goods 的 JSON 序列化 */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /** 分布式锁释放脚本：CAS 删除 */
    @Bean
    public DefaultRedisScript<Long> lockUnlockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "  return redis.call('del', KEYS[1]) " +
                "else return 0 end");
        script.setResultType(Long.class);
        return script;
    }

    /** 令牌桶算法限流 Lua 脚本 */
    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "local key = KEYS[1] " +
                "local capacity = tonumber(ARGV[1]) " +
                "local rate = tonumber(ARGV[2]) " +
                "local requested = tonumber(ARGV[3]) " +
                "local now = tonumber(ARGV[4]) " +
                "local last_tokens = tonumber(redis.call('hget', key, 'tokens')) " +
                "local last_refreshed = tonumber(redis.call('hget', key, 'last_refreshed')) " +
                "if last_tokens == nil then " +
                "  last_tokens = capacity " +
                "  last_refreshed = now " +
                "end " +
                "local delta = math.max(0, now - last_refreshed) " +
                "local tokens = math.min(capacity, last_tokens + delta * rate) " +
                "local allowed = 0 " +
                "if tokens >= requested then " +
                "  tokens = tokens - requested " +
                "  allowed = 1 " +
                "end " +
                "redis.call('hset', key, 'tokens', tokens) " +
                "redis.call('hset', key, 'last_refreshed', now) " +
                "redis.call('expire', key, 30) " +
                "return allowed");
        script.setResultType(Long.class);
        return script;
    }

    /** Redis Lua 预减库存脚本 */
    @Bean
    public DefaultRedisScript<Long> seckillStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "local stockKey = KEYS[1] " +
                "local quantity = tonumber(ARGV[1]) " +
                "local stock = tonumber(redis.call('get', stockKey)) " +
                "if stock == nil then " +
                "  return -1 " + // 标识 Redis 中未预热商品库存
                "end " +
                "if stock < quantity then " +
                "  return 0 " + // 标识库存不足
                "end " +
                "redis.call('decrby', stockKey, quantity) " +
                "return 1"); // 预减库存成功
        script.setResultType(Long.class);
        return script;
    }
}
