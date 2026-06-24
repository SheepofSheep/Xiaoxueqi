package com.taiyuan.food.dto;

import lombok.Data;

@Data
public class RestaurantQueryDTO {
    private String keyword;
    private String district;
    private String businessArea;
    private String cuisine;
    private Double maxPrice;
    private String tasteTag;
    private String sceneTag;
    private Integer page = 1;
    private Integer pageSize = 10;
}
