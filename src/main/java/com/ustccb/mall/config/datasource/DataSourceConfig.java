package com.ustccb.mall.config.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 读写分离多数据源配置类
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public DataSource dynamicDataSource(DataSource masterDataSource, DataSource slaveDataSource) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        // 默认数据源为 master
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource);
        
        // 配置目标数据源集合
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(DbContextHolder.WRITE_DATASOURCE, masterDataSource);
        dataSourceMap.put(DbContextHolder.READ_DATASOURCE, slaveDataSource);
        dynamicDataSource.setTargetDataSources(dataSourceMap);
        
        return dynamicDataSource;
    }
}
