package com.ustccb.mall.aspect;

import com.ustccb.mall.annotation.RateLimit;
import com.ustccb.mall.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * 分布式限流切面：结合 AOP + SpEL + Redis Lua 令牌桶算法
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> rateLimitScript;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        // 1. 初始化 SpEL 上下文，将方法参数名与值绑定，供表达式动态解析
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = pjp.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 2. 解析 SpEL 表达式中的动态 Key（如：#goodsId -> 获取对应方法参数值）
        String rawKey = rateLimit.key();
        String parsedKey = rawKey;
        if (rawKey.contains("#")) {
            try {
                Expression expression = parser.parseExpression(rawKey);
                parsedKey = expression.getValue(context, String.class);
            } catch (Exception e) {
                log.error("解析 SpEL 表达式失败: {}", rawKey, e);
            }
        }

        String limitKey = "mall:ratelimit:" + parsedKey;

        // 3. 组装限流令牌桶参数
        String capacity = String.valueOf(rateLimit.capacity());
        String rate = String.valueOf(rateLimit.qps());
        String requested = "1";
        // 传入当前秒级时间戳，计算生成令牌数
        String now = String.valueOf(System.currentTimeMillis() / 1000); 

        // 4. 调用 Redis 执行 Lua 限流脚本
        Long allowed = redis.execute(
                rateLimitScript,
                Collections.singletonList(limitKey),
                capacity,
                rate,
                requested,
                now
        );

        if (allowed == null || allowed == 0L) {
            log.warn("[RateLimitAspect] 触发限流拦截: key={}", limitKey);
            throw new BizException(rateLimit.message());
        }

        return pjp.proceed();
    }
}
