package com.taiyuan.food.vo;

import java.math.BigDecimal;
import java.util.List;

public record AiFoodIntentVO(
    String district,
    String businessArea,
    BigDecimal maxPrice,
    List<String> cuisines,
    List<String> tasteTags,
    List<String> sceneTags,
    List<String> avoidTags,
    String summary,
    boolean aiUsed,
    boolean aiFallback,
    String aiModel,
    long aiLatencyMs
) {
}
