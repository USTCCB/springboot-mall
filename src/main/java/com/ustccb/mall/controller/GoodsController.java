package com.ustccb.mall.controller;

import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品接口")
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "商品列表")
    @GetMapping
    public List<Goods> list() { return goodsService.list(); }

    @Operation(summary = "商品详情（Cache Aside）")
    @GetMapping("/{id}")
    public Goods get(@PathVariable Long id) { return goodsService.get(id); }

    @Operation(summary = "更新商品（清缓存）")
    @PutMapping("/{id}")
    public Goods update(@PathVariable Long id, @RequestBody Goods g) {
        return goodsService.update(id, g);
    }
}
