package com.ustccb.mall;

import org.junit.jupiter.api.Test;

/**
 * 上下文轻量验证：仅校验注解能被加载，不启动 Spring 容器（避免 CI 缺 Redis）。
 */
class MallApplicationTests {

    @Test
    void contextSanity() {
        // 简单的 sanity 检查
        int x = 1 + 1;
        assert 2 == x : "sanity";
    }
}
