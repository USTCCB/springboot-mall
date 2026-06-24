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
}
