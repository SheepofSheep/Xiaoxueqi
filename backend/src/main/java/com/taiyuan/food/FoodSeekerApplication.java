package com.taiyuan.food;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.taiyuan.food.mapper")
public class FoodSeekerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodSeekerApplication.class, args);
    }
}
