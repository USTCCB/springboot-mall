package com.ustccb.mall.mapper;
import com.ustccb.mall.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface GoodsMapper {
    List<Goods> findAll();
    Goods findById(@Param("id") Long id);
    int decreaseStock(@Param("id") Long id, @Param("qty") int qty);
}
