package com.taiyuan.food.vo;

import java.util.List;

public record AiTagSuggestionVO(
    List<String> tasteTags,
    List<String> sceneTags,
    List<String> avoidTags,
    int confidence,
    boolean aiUsed,
    boolean aiFallback,
    String aiModel,
    long aiLatencyMs
) {}
