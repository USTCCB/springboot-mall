package com.ustccb.mall.controller;

import com.ustccb.mall.annotation.RateLimit;
import com.ustccb.mall.entity.MallOrder;
import com.ustccb.mall.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 */
@Tag(name = "订单接口")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单（同步常规模式）")
    @PostMapping
    public MallOrder create(
            @Parameter(description = "用户 ID") @RequestParam Long userId,
            @Parameter(description = "商品 ID") @RequestParam Long goodsId,
            @Parameter(description = "数量")     @RequestParam Integer quantity) {
        return orderService.create(userId, goodsId, quantity);
    }

    /**
     * 高并发秒杀抢购入口：
     * 1. 采用 @RateLimit 分布式令牌桶限流，QPS为2，限制单用户对单个商品的瞬时频繁点击
     * 2. 内部采用 Redis Lua 库存预扣减 + RabbitMQ 异步落库削峰
     */
    @Operation(summary = "创建订单（高并发秒杀异步削峰模式）")
    @PostMapping("/seckill")
    @RateLimit(key = "'seckill:' + #goodsId + ':' + #userId", qps = 2.0, capacity = 5.0, message = "秒杀抢购过于火爆，你已被限流，请稍后再试")
    public MallOrder createSeckill(
            @Parameter(description = "用户 ID") @RequestParam Long userId,
            @Parameter(description = "商品 ID") @RequestParam Long goodsId,
            @Parameter(description = "数量")     @RequestParam Integer quantity) {
        return orderService.createSeckill(userId, goodsId, quantity);
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{id}/pay")
    public MallOrder pay(@PathVariable Long id) {
        return orderService.pay(id);
    }
}
