package com.ustccb.mall.controller;
import com.ustccb.mall.entity.MallOrder;
import com.ustccb.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    @PostMapping
    public MallOrder create(@RequestBody Map<String,Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long goodsId = ((Number) body.get("goodsId")).longValue();
        Integer quantity = ((Number) body.get("quantity")).intValue();
        return orderService.create(userId, goodsId, quantity);
    }
    @PostMapping("/{id}/pay")
    public MallOrder pay(@PathVariable Long id) { return orderService.pay(id); }
}
