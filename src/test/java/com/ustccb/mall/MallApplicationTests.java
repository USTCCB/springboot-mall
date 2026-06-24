package com.ustccb.mall;
import com.ustccb.mall.service.GoodsService;
import com.ustccb.mall.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class MallApplicationTests {
    @Autowired GoodsService goodsService;
    @Autowired OrderService orderService;
    @Test
    void smoke() {
        assertTrue(goodsService.list().size() >= 3);
        var o = orderService.create(1L, 1L, 1);
        assertNotNull(o.getId());
    }
}
