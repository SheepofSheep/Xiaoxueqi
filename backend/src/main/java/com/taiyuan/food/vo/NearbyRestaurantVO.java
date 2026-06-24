package com.taiyuan.food.vo;

import java.math.BigDecimal;

public record NearbyRestaurantVO(
    String id,
    String name,
    String address,
    int distance,
    String type,
    BigDecimal longitude,
    BigDecimal latitude,
    String source,
    boolean canRecommend,
    String coverImage,
    BigDecimal rating
) {}
