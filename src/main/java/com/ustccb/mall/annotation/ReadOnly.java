package com.ustccb.mall.annotation;

import java.lang.annotation.*;

/**
 * 只读注解：标识查询操作，路由至从数据库 (slave)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
}
