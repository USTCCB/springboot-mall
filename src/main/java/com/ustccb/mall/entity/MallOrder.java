package com.ustccb.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 */
@Data
@TableName("mall_order")
public class MallOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long goodsId;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
