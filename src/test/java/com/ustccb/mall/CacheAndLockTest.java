package com.ustccb.mall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.mapper.GoodsMapper;
import com.ustccb.mall.service.GoodsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 缓存防空与并发锁防护单元测试 (基于 Mock 验证)
 */
@ExtendWith(MockitoExtension.class)
class CacheAndLockTest {

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedissonClient redisson;

    @Mock
    private RLock rLock;

    @InjectMocks
    private GoodsService goodsService;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetCacheHit() throws Exception {
        // GIVEN: 缓存命中
        String cacheKey = "mall:goods:1";
        Goods mockGoods = new Goods();
        mockGoods.setId(1L);
        mockGoods.setTitle("Test Goods");

        when(valueOperations.get(cacheKey)).thenReturn("{\"id\":1,\"title\":\"Test Goods\"}");
        when(objectMapper.readValue(anyString(), eq(Goods.class))).thenReturn(mockGoods);

        // WHEN
        Goods result = goodsService.get(1L);

        // THEN
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(goodsMapper, never()).findById(anyLong()); // 缓存命中，不查 DB
        verify(redisson, never()).getLock(anyString()); // 不加锁
    }

    @Test
    void testGetCacheMissAndDatabaseHit() throws Exception {
        // GIVEN: 缓存穿透/失效，DB 命中
        String cacheKey = "mall:goods:1";
        String lockKey = "mall:goods:lock:1";
        Goods mockGoods = new Goods();
        mockGoods.setId(1L);
        mockGoods.setTitle("Test Goods");

        // 首次缓存未命中
        when(valueOperations.get(cacheKey)).thenReturn(null);
        // 分布式锁获取成功
        when(redisson.getLock(lockKey)).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // DB 命中
        when(goodsMapper.findById(1L)).thenReturn(mockGoods);
        when(objectMapper.writeValueAsString(mockGoods)).thenReturn("{\"id\":1}");

        // WHEN
        Goods result = goodsService.get(1L);

        // THEN
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(goodsMapper, times(1)).findById(1L);
        verify(valueOperations, times(1)).set(eq(cacheKey), anyString(), any(Duration.class)); // 回写缓存
        verify(rLock, times(1)).unlock(); // 释放锁
    }

    @Test
    void testGetCachePenetrationPrevention() throws Exception {
        // GIVEN: 缓存未命中，且 DB 也不存在该商品（穿透场景）
        String cacheKey = "mall:goods:999";
        String lockKey = "mall:goods:lock:999";

        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(redisson.getLock(lockKey)).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // DB 未命中
        when(goodsMapper.findById(999L)).thenReturn(null);

        // WHEN
        Goods result = goodsService.get(999L);

        // THEN
        assertNull(result);
        verify(goodsMapper, times(1)).findById(999L);
        // 应回写 "{}" 空缓存标记，过期时间设为 60s
        verify(valueOperations, times(1)).set(eq(cacheKey), eq("{}"), eq(Duration.ofSeconds(60)));
        verify(rLock, times(1)).unlock();
    }

    @Test
    void testGetCacheHitEmptyValue() {
        // GIVEN: 缓存命中空标记 "{}"，直接判定商品不存在，有效拦截穿透
        String cacheKey = "mall:goods:999";
        when(valueOperations.get(cacheKey)).thenReturn("{}");

        // WHEN
        Goods result = goodsService.get(999L);

        // THEN
        assertNull(result);
        verify(goodsMapper, never()).findById(anyLong()); // 拦截穿透，不查 DB
        verify(redisson, never()).getLock(anyString()); // 不加锁
    }
}
