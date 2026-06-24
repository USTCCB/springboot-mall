package com.ustccb.mall.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustccb.mall.annotation.Idempotent;
import com.ustccb.mall.config.RabbitMQConfig;
import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.entity.MallOrder;
import com.ustccb.mall.exception.BizException;
import com.ustccb.mall.mapper.GoodsMapper;
import com.ustccb.mall.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务：包含常规同步下单与高性能 Redis Lua + RabbitMQ 异步下单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String LOCK_PREFIX = "mall:order:lock:";

    private final GoodsMapper goodsMapper;
    private final OrderMapper orderMapper;
    private final RedissonClient redisson;
    private final TransactionTemplate transactionTemplate;
    
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> seckillStockScript;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建订单：常规同步模式（结合 Redisson 锁 + 编程式事务）
     */
    @Idempotent(expireSeconds = 30, message = "请勿重复下单")
    public MallOrder create(Long userId, Long goodsId, Integer quantity) {
        String lockKey = LOCK_PREFIX + goodsId;
        RLock lock = redisson.getLock(lockKey);

        try {
            // 最多等待 3 秒获取锁
            boolean locked = lock.tryLock(3, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("并发获取分布式锁失败 goodsId={}", goodsId);
                throw new BizException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("下单请求中断，请稍后重试");
        }

        try {
            // 编程式事务：保证 doCreate 原子逻辑成功，并在锁释放前完成 Commit
            return transactionTemplate.execute(status -> doCreate(userId, goodsId, quantity));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("成功释放分布式锁 key={}", lockKey);
            }
        }
    }

    /**
     * 高并发秒杀下单：高性能异步削峰模式（Redis Lua 预扣减库存 + RabbitMQ 消息队列）
     */
    public MallOrder createSeckill(Long userId, Long goodsId, Integer quantity) {
        String stockKey = "mall:goods:stock:" + goodsId;

        // 1. 调用 Redis Lua 脚本原子性扣减 Redis 缓存中的库存
        Long result = redis.execute(
                seckillStockScript,
                Collections.singletonList(stockKey),
                String.valueOf(quantity)
        );

        if (result == null || result == -1L) {
            // 缓存库存未初始化（懒加载加载）: 从数据库查询真实库存回写至 Redis
            Goods g = goodsMapper.findById(goodsId);
            if (g == null) {
                throw new BizException("商品不存在");
            }
            redis.opsForValue().setIfAbsent(stockKey, String.valueOf(g.getStock()), Duration.ofHours(2));
            // 重新尝试扣减
            result = redis.execute(seckillStockScript, Collections.singletonList(stockKey), String.valueOf(quantity));
        }

        if (result == null || result == 0L) {
            throw new BizException("库存不足，秒杀已结束");
        }

        // 2. 预减库存成功，异步向 RabbitMQ 发送下单通知 (MQ 削峰填谷)
        // 生成全局唯一的消费端幂等主键 Key
        String idempotentKey = System.currentTimeMillis() + ":" + userId + ":" + goodsId;
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("goodsId", goodsId);
        payload.put("quantity", quantity);
        payload.put("idempotentKey", idempotentKey);

        try {
            String jsonMessage = objectMapper.writeValueAsString(payload);
            // 投递消息至 RabbitMQ 交换机
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_ROUTING_KEY,
                    jsonMessage
            );
            log.info("[Seckill Engine] 消息成功投递至 MQ: key={}", idempotentKey);
        } catch (Exception e) {
            log.error("[Seckill Engine] MQ 异步消息投递异常，执行 Redis 库存回滚补偿. key={}", idempotentKey, e);
            // 出现消息投递失败时，主动把 Redis 预扣除的库存回滚（高可靠一致性闭环）
            redis.opsForValue().increment(stockKey, quantity);
            throw new BizException("服务器繁忙，请稍后再试");
        }

        // 3. 快速响应客户端，返回一个 "PENDING" (处理中) 的占位订单对象，供前端轮询查询真实订单状态
        MallOrder mockOrder = new MallOrder();
        mockOrder.setId(-1L); // 特殊标记 ID，表示处于 MQ 异步处理队列中
        mockOrder.setUserId(userId);
        mockOrder.setGoodsId(goodsId);
        mockOrder.setQuantity(quantity);
        mockOrder.setStatus("PENDING");
        return mockOrder;
    }

    /**
     * 核心落库方法：执行实际扣减数据库库存和创建订单 (普通下单/异步消费者均共用此底层方法)
     */
    public MallOrder doCreate(Long userId, Long goodsId, Integer quantity) {
        // 利用底层 H2/MySQL 的数据库行级排他锁 + 行库存字段乐观校验，进行数据库防超卖
        int rows = goodsMapper.decreaseStock(goodsId, quantity);
        if (rows == 0) {
            throw new BizException("库存不足");
        }
        
        Goods g = goodsMapper.findById(goodsId);
        BigDecimal amount = g.getPrice().multiply(BigDecimal.valueOf(quantity));

        MallOrder o = new MallOrder();
        o.setUserId(userId);
        o.setGoodsId(goodsId);
        o.setQuantity(quantity);
        o.setAmount(amount);
        o.setStatus("PENDING");
        orderMapper.insert(o);
        log.info("[DB Order] 数据库落库成功: orderId={}, goodsId={}", o.getId(), goodsId);
        return o;
    }

    public MallOrder pay(Long id) {
        orderMapper.updateStatus(id, "PAID");
        return orderMapper.findById(id);
    }
}
