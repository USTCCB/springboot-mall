package com.ustccb.mall.service;
import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.entity.MallOrder;
import com.ustccb.mall.mapper.GoodsMapper;
import com.ustccb.mall.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final GoodsMapper goodsMapper;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate redis;
    @Transactional
    public MallOrder create(Long userId, Long goodsId, Integer quantity) {
        String key = "mall:order:lock:" + userId + ":" + goodsId;
        if (Boolean.TRUE.equals(redis.hasKey(key))) {
            throw new RuntimeException("操作太快，请稍后");
        }
        redis.opsForValue().set(key, "1", Duration.ofSeconds(2));
        int rows = goodsMapper.decreaseStock(goodsId, quantity);
        if (rows == 0) throw new RuntimeException("库存不足");
        Goods g = goodsMapper.findById(goodsId);
        BigDecimal amount = g.getPrice().multiply(BigDecimal.valueOf(quantity));
        MallOrder o = new MallOrder();
        o.setUserId(userId); o.setGoodsId(goodsId);
        o.setQuantity(quantity); o.setAmount(amount);
        o.setStatus("PENDING");
        orderMapper.insert(o);
        log.info("create mall order: {}", o.getId());
        return o;
    }
    public MallOrder pay(Long id) {
        orderMapper.updateStatus(id, "PAID");
        return orderMapper.findById(id);
    }
}
