package com.ustccb.mall.service;
import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;
@Service
@RequiredArgsConstructor
public class GoodsService {
    private final GoodsMapper goodsMapper;
    private final StringRedisTemplate redis;
    public List<Goods> list() { return goodsMapper.findAll(); }
    public Goods get(Long id) { return goodsMapper.findById(id); }
}
