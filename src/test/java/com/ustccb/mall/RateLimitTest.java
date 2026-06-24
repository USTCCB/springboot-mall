package com.ustccb.mall;

import com.ustccb.mall.controller.OrderController;
import com.ustccb.mall.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SpringBootTest
class RateLimitTest {

    @Autowired
    private OrderController orderController;

    @MockBean
    private StringRedisTemplate redis;

    @MockBean(name = "rateLimitScript")
    private DefaultRedisScript<Long> rateLimitScript;

    @Test
    void testRateLimitAspectThrowsException() {
        // GIVEN: 模拟 Redis 执行限流 Lua 脚本返回 0L (表示令牌不足被限流)
        when(redis.execute(
                any(DefaultRedisScript.class),
                anyList(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(0L);

        // WHEN & THEN: 并发访问秒杀下单接口应当拦截并抛出被限流的 BizException
        assertThrows(BizException.class, () -> {
            orderController.createSeckill(1L, 1L, 1);
        });
    }
}
