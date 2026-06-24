package com.ustccb.mall.mapper;
import com.ustccb.mall.entity.MallOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface OrderMapper {
    int insert(MallOrder o);
    MallOrder findById(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
