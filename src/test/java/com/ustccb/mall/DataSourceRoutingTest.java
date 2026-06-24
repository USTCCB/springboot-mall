package com.ustccb.mall;

import com.ustccb.mall.config.datasource.DbContextHolder;
import com.ustccb.mall.service.GoodsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class DataSourceRoutingTest {

    @Autowired
    private GoodsService goodsService;

    @Test
    void testReadOnlyRoutingContextClear() {
        // GIVEN: 运行前 ThreadLocal 应该为空 (使用默认 Master)
        assertNull(DbContextHolder.getDbType());

        // WHEN: 执行标注了 @ReadOnly 的查询方法
        goodsService.list();

        // THEN: 执行后切面应当安全清除 ThreadLocal 变量以防止内存泄漏，上下文应恢复为 null
        assertNull(DbContextHolder.getDbType());
    }
}
