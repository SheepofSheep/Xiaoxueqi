package com.taiyuan.food.vo;

public record AiReasonVO(
    String reason,
    boolean aiUsed,
    boolean aiFallback,
    String aiModel,
    long aiLatencyMs
) {}
