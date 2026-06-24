package com.taiyuan.food.vo;

public record ShakeResultVO(
    RestaurantVO restaurant,
    int matchScore,
    String reason,
    String clientTrigger
) {
}
