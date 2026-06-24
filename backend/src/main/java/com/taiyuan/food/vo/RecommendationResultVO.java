package com.taiyuan.food.vo;

import java.util.List;

public record RecommendationResultVO(
    List<RecommendationItemVO> items,
    boolean relaxed,
    List<String> relaxedRules,
    boolean localReasonUsed,
    boolean aiUsed,
    boolean aiFallback
) {
}
