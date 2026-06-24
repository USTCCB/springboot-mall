package com.ustccb.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品实体类
 */
@Data
@TableName("goods")
public class Goods {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private BigDecimal price;
    private Integer stock;
    private String description;
}
