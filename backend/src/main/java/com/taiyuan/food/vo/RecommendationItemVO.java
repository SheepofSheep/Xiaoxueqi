package com.taiyuan.food.vo;

import java.util.List;

public record RecommendationItemVO(
    RestaurantVO restaurant,
    int matchScore,
    List<String> matchedTags,
    String reason,
    boolean aiUsed,
    boolean aiFallback,
    String aiModel,
    long aiLatencyMs
) {
}
