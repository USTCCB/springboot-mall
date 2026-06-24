package com.ustccb.mall;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@MapperScan("com.ustccb.mall.mapper")
public class MallApplication {
    public static void main(String[] args) { SpringApplication.run(MallApplication.class, args); }
}
