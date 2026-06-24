package com.taiyuan.food.dto;

import java.util.List;

public record AiTagSuggestionRequestDTO(
    String name,
    String cuisine,
    int averagePrice,
    String description,
    List<String> allowedTasteTags,
    List<String> allowedSceneTags
) {}
