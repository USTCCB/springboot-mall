package com.ustccb.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ustccb.mall.entity.MallOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单 Mapper：继承 MyBatis-Plus 的 BaseMapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<MallOrder> {
    MallOrder findById(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
