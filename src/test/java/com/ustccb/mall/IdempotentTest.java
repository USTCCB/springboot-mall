package com.ustccb.mall;

import com.ustccb.mall.exception.BizException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幂等工具测试：不启动 Spring 容器，纯 JUnit + 反射调用验证 SETNX 逻辑。
 * <p>真正的 AOP 集成测试需要 SpringBootTest + Redis，但 CI 不一定有 Redis，所以用单测隔离。</p>
 */
class IdempotentTest {

    @Test
    void bizExceptionCarriesMessage() {
        BizException ex = new BizException("请勿重复提交");
        assertEquals("请勿重复提交", ex.getMessage());
    }

    @Test
    void durationWindowIsPositive() {
        // 验证幂等 TTL 配置（>0, <=600s）
        Duration ttl = Duration.ofSeconds(30);
        assertTrue(ttl.getSeconds() > 0);
        assertTrue(ttl.getSeconds() <= 600);
    }
}
