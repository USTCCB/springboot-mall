package com.ustccb.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ustccb.mall.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 商品 Mapper：继承 MyBatis-Plus 的 BaseMapper 具备基础 CRUD，并保留高并发自定义 SQL
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
    List<Goods> findAll();
    Goods findById(@Param("id") Long id);
    int decreaseStock(@Param("id") Long id, @Param("qty") int qty);
}
