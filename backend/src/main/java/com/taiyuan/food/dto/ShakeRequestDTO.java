package com.taiyuan.food.dto;

import java.math.BigDecimal;
import java.util.List;

public record ShakeRequestDTO(
    String district,
    String businessArea,
    BigDecimal maxPrice,
    List<String> avoidTags,
    List<Long> excludeRestaurantIds,
    String clientTrigger
) {
}
