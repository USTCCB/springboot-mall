package com.ustccb.mall.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class MallOrder {
    private Long id;
    private Long userId;
    private Long goodsId;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
