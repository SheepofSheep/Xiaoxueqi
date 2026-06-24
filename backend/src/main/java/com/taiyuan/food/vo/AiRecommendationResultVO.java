package com.taiyuan.food.vo;

import com.taiyuan.food.dto.RecommendationRequestDTO;

public record AiRecommendationResultVO(
    RecommendationResultVO recommendation,
    RecommendationRequestDTO interpretedRequest,
    AiFoodIntentVO intent
) {
}
