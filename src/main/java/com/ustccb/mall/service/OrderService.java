package com.ustccb.mall.service;

import com.ustccb.mall.annotation.Idempotent;
import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.entity.MallOrder;
import com.ustccb.mall.exception.BizException;
import com.ustccb.mall.mapper.GoodsMapper;
import com.ustccb.mall.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

/**
 * 订单服务
 * <ul>
 *   <li>Redis 分布式锁（SETNX + UUID + Lua 释放）保证同商品并发安全</li>
 *   <li>@Idempotent 注解：HTTP 层防重（header X-Idempotent-Key）</li>
 *   <li>@Transactional：订单 + 库存原子性</li>
 *   <li>条件 UPDATE 防超卖：UPDATE goods SET stock = stock - ? WHERE id = ? AND stock >= ?</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String LOCK_PREFIX = "mall:order:lock:";
    private static final long LOCK_TTL_SEC = 5;

    /** Lua: 仅当 value 匹配时才删除（防止误删别人的锁） */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    private final GoodsMapper goodsMapper;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> unlockScript;

    /**
     * 创建订单
     *
     * @param userId    用户 ID
     * @param goodsId   商品 ID
     * @param quantity  数量
     * @return 新订单
     */
    @Idempotent(expireSeconds = 30, message = "请勿重复下单")
    public MallOrder create(Long userId, Long goodsId, Integer quantity) {
        String lockKey = LOCK_PREFIX + goodsId;
        String lockVal = UUID.randomUUID().toString();

        // 1) 抢分布式锁
        Boolean locked = redis.opsForValue()
                .setIfAbsent(lockKey, lockVal, java.time.Duration.ofSeconds(LOCK_TTL_SEC));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException("系统繁忙，请稍后重试");
        }

        try {
            return doCreate(userId, goodsId, quantity);
        } finally {
            // 2) Lua 释放（仅自己持有时才删）
            Long released = redis.execute(unlockScript, Collections.singletonList(lockKey), lockVal);
            log.debug("释放分布式锁 key={} result={}", lockKey, released);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected MallOrder doCreate(Long userId, Long goodsId, Integer quantity) {
        // 3) 条件 UPDATE 扣库存（防超卖）
        int rows = goodsMapper.decreaseStock(goodsId, quantity);
        if (rows == 0) {
            throw new BizException("库存不足");
        }
        // 4) 算金额
        Goods g = goodsMapper.findById(goodsId);
        BigDecimal amount = g.getPrice().multiply(BigDecimal.valueOf(quantity));
        // 5) 落单
        MallOrder o = new MallOrder();
        o.setUserId(userId);
        o.setGoodsId(goodsId);
        o.setQuantity(quantity);
        o.setAmount(amount);
        o.setStatus("PENDING");
        orderMapper.insert(o);
        log.info("下单成功 orderId={} userId={} goodsId={} qty={}", o.getId(), userId, goodsId, quantity);
        return o;
    }

    public MallOrder pay(Long id) {
        orderMapper.updateStatus(id, "PAID");
        return orderMapper.findById(id);
    }
}
