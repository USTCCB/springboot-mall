package com.ustccb.mall.entity;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class Goods {
    private Long id;
    private String title;
    private BigDecimal price;
    private Integer stock;
    private String description;
}
