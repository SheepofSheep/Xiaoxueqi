package com.taiyuan.food.vo;

import java.util.List;

public record NearbyResultVO(
    List<NearbyRestaurantVO> list,
    int page,
    int pageSize,
    int total,
    String notice
) {}
