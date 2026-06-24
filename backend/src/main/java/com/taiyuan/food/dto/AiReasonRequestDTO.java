package com.taiyuan.food.dto;

import java.util.List;

public record AiReasonRequestDTO(
    String restaurantName,
    String businessArea,
    int averagePrice,
    String cuisine,
    List<String> matchedTags,
    String localReason
) {}
