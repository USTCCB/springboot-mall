package com.ustccb.mall.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由实现：继承 Spring 的 AbstractRoutingDataSource 并重写 determineCurrentLookupKey
 */
public class DynamicDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        // 根据 ThreadLocal 中的值来决定本次数据库操作路由到哪个数据源
        return DbContextHolder.getDbType();
    }
}
