package com.ustccb.mall.service;

import com.ustccb.mall.annotation.Idempotent;
import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.entity.MallOrder;
import com.ustccb.mall.exception.BizException;
import com.ustccb.mall.mapper.GoodsMapper;
import com.ustccb.mall.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务
 * <ul>
 *   <li>Redisson 分布式锁（RLock）保证同商品并发安全，支持可重入、等待重试和 Watchdog 自动续期</li>
 *   <li>TransactionTemplate 编程式事务：解决 AOP 自调用失效问题，并确保事务在锁释放前提交，防止并发穿透和超卖</li>
 *   <li>@Idempotent 注解：HTTP 层接口幂等防重（header X-Idempotent-Key）</li>
 *   <li>条件 UPDATE 防超卖：UPDATE goods SET stock = stock - ? WHERE id = ? AND stock >= ?</li>
 * </ul>
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
        RLock lock = redisson.getLock(lockKey);

        try {
            // 抢分布式锁：最多等待 3 秒，抢到锁后如果不指定 leaseTime，则使用 Redisson Watchdog 自动续期（每 10 秒续期到 30 秒）
            // 简历亮点：支持等待重试（高并发下防止大批请求一瞬间因为锁冲突全部被拒，提升用户体验）
            boolean locked = lock.tryLock(3, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取分布式锁失败 goodsId={}", goodsId);
                throw new BizException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("下单请求中断，请稍后重试");
        }

        try {
            // 使用 TransactionTemplate 编程式事务，确保：
            // 1. doCreate 逻辑在事务中运行并落库
            // 2. 事务在 finally 释放分布式锁之前成功提交，避免“锁释放了但事务还没提交，其他请求获取到锁读到旧数据”的经典并发漏洞
            return transactionTemplate.execute(status -> doCreate(userId, goodsId, quantity));
        } finally {
            // 仅释放当前线程持有的锁，防止锁超时释放后误解锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("成功释放分布式锁 key={}", lockKey);
            }
        }
    }

    /**
     * 核心下单逻辑：扣减库存与生成订单（由 TransactionTemplate 统一保证事务原子性）
     */
    public MallOrder doCreate(Long userId, Long goodsId, Integer quantity) {
        // 1. 条件 UPDATE 扣减库存，利用底层数据库行锁防超卖
        int rows = goodsMapper.decreaseStock(goodsId, quantity);
        if (rows == 0) {
            throw new BizException("库存不足");
        }
        // 2. 算金额
        Goods g = goodsMapper.findById(goodsId);
        BigDecimal amount = g.getPrice().multiply(BigDecimal.valueOf(quantity));
        // 3. 落单
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

