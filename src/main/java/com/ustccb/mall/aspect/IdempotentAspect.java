package com.ustccb.mall.aspect;

import com.ustccb.mall.annotation.Idempotent;
import com.ustccb.mall.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Objects;

/**
 * 幂等切面：基于 Redis SETNX 实现
 * <pre>
 *   1. 取 header 中的 token (X-Idempotent-Key)
 *   2. SETNX 到 Redis，TTL = expireSeconds
 *   3. 返回 true -> 首次请求，放行
 *   4. 返回 false -> 重复请求，抛 BizException
 * </pre>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate redis;

    @Around("@annotation(com.ustccb.mall.annotation.Idempotent)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Idempotent anno = sig.getMethod().getAnnotation(Idempotent.class);

        HttpServletRequest req = currentRequest();
        String token = req == null ? null : req.getHeader(anno.headerKey());

        if (token == null || token.isBlank()) {
            // 没有 token 视为首次，但记 warning（生产应直接拒绝）
            log.warn("[Idempotent] {} 缺少幂等 token {}，放行", sig.getName(), anno.headerKey());
            return pjp.proceed();
        }

        String key = anno.prefix() + token;
        Boolean firstTime = redis.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(anno.expireSeconds()));

        if (Boolean.FALSE.equals(firstTime)) {
            log.info("[Idempotent] 重复请求被拒: key={}", key);
            throw new BizException(anno.message());
        }

        try {
            return pjp.proceed();
        } catch (Throwable t) {
            // 业务失败，删除 key，让客户端可以重试
            redis.delete(key);
            throw t;
        }
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}
