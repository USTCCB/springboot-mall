package com.ustccb.mall.controller;
import com.ustccb.mall.entity.Goods;
import com.ustccb.mall.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {
    private final GoodsService goodsService;
    @GetMapping public List<Goods> list() { return goodsService.list(); }
    @GetMapping("/{id}") public Goods get(@PathVariable Long id) { return goodsService.get(id); }
}
