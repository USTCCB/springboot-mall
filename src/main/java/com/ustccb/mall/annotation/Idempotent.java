package com.ustccb.mall.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等性注解
 * <p>基于 Redis + Token 实现：调用方在 header 传入唯一 token (X-Idempotent-Key)，
 * 服务端首次写入 Redis（TTL = expireSeconds），重复请求直接拒绝，避免重复下单等副作用。</p>
 *
 * @author 陈彪
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /** 幂等 key 在 header 中的名称 */
    String headerKey() default "X-Idempotent-Key";
    /** Redis 缓存前缀 */
    String prefix() default "idempotent:";
    /** 过期时间（秒） */
    int expireSeconds() default 60;
    /** 重复请求时的提示信息 */
    String message() default "请勿重复提交";
}
