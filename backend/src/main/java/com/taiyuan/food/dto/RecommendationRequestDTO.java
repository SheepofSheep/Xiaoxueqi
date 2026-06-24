package com.taiyuan.food.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationRequestDTO(
    String district,
    String businessArea,
    BigDecimal maxPrice,
    List<String> cuisines,
    List<String> tasteTags,
    List<String> sceneTags,
    List<String> avoidTags,
    Integer peopleCount,
    Integer limit,
    List<Long> excludeRestaurantIds
) {
}
