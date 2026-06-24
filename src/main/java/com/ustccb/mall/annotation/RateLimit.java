package com.ustccb.mall.annotation;

import java.lang.annotation.*;

/**
 * 分布式令牌桶限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 限流 Key，支持 SpEL 动态解析（例如："'goods:' + #goodsId"）
     */
    String key() default "";

    /**
     * 每秒放入令牌桶中的令牌数 (QPS)
     */
    double qps() default 1.0;

    /**
     * 令牌桶最大容量
     */
    double capacity() default 5.0;

    /**
     * 限流被拦截后的异常提示信息
     */
    String message() default "请求过快，已被限流";
}
