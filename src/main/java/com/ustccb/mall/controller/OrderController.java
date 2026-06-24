package com.ustccb.mall.controller;

import com.ustccb.mall.entity.MallOrder;
import com.ustccb.mall.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 * <p>POST /api/orders 自动启用接口幂等：请求需带 X-Idempotent-Key header，30 秒内重复请求被拒。</p>
 */
@Tag(name = "订单接口")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单（幂等）")
    @PostMapping
    public MallOrder create(
            @Parameter(description = "用户 ID") @RequestParam Long userId,
            @Parameter(description = "商品 ID") @RequestParam Long goodsId,
            @Parameter(description = "数量")     @RequestParam Integer quantity) {
        return orderService.create(userId, goodsId, quantity);
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{id}/pay")
    public MallOrder pay(@PathVariable Long id) {
        return orderService.pay(id);
    }
}
