package com.ustccb.mall.aspect;

import com.ustccb.mall.config.datasource.DbContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据源动态切换切面
 */
@Slf4j
@Aspect
@Component
@Order(1) // 极其关键的细节：必须保证在事务切面（TransactionInterceptor）之前执行，否则 Spring 在开启事务时已经获取了默认的 Connection，路由切换将失效！
public class DataSourceAspect {

    @Around("@annotation(com.ustccb.mall.annotation.ReadOnly) || @within(com.ustccb.mall.annotation.ReadOnly)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        log.debug("[DataSourceAspect] 路由至只读从数据库 (slave)");
        DbContextHolder.setDbType(DbContextHolder.READ_DATASOURCE);
        try {
            return pjp.proceed();
        } finally {
            DbContextHolder.clearDbType();
            log.debug("[DataSourceAspect] 清除当前只读数据源上下文");
        }
    }
}
