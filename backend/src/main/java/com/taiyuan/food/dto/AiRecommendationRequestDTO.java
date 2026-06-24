package com.taiyuan.food.dto;

import java.math.BigDecimal;

public record AiRecommendationRequestDTO(
    String prompt,
    String district,
    String businessArea,
    BigDecimal maxPrice,
    Integer limit
) {
}
