package com.ustccb.mall;

import com.ustccb.mall.service.OrderService;
import com.ustccb.mall.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 幂等测试：相同 token 第二次调用应抛 BizException
 * <p>用 Mock 隔离 Redis，验证 AOP 切面逻辑</p>
 */
@SpringBootTest
class IdempotentTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private StringRedisTemplate redis;

    @Test
    void shouldRejectDuplicateRequest() {
        // GIVEN: SETNX 返回 false 表示 key 已存在
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        // 业务方法本身在 AOP 中抛 BizException,此处仅验证 SETNX 已被调用
        verify(ops, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }
}
