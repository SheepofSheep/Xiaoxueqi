package com.taiyuan.food.dto;

import java.math.BigDecimal;
import java.util.List;

public record AiFoodIntentRequestDTO(
    String prompt,
    String defaultDistrict,
    String defaultBusinessArea,
    BigDecimal defaultMaxPrice,
    List<String> allowedCuisines,
    List<String> allowedTasteTags,
    List<String> allowedSceneTags,
    List<String> allowedAvoidTags
) {
}
