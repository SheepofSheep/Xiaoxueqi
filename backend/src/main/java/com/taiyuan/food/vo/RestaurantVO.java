package com.taiyuan.food.vo;

import com.taiyuan.food.common.TagConverter;
import com.taiyuan.food.entity.RestaurantEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RestaurantVO(
    Long id,
    String name,
    String district,
    String businessArea,
    String address,
    BigDecimal averagePrice,
    String cuisine,
    BigDecimal rating,
    List<String> tasteTags,
    List<String> sceneTags,
    List<String> avoidTags,
    List<String> recommendedDishes,
    String description,
    BigDecimal latitude,
    BigDecimal longitude,
    String coverImage,
    String source,
    String sourceNote,
    boolean isDemoData,
    int status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static RestaurantVO from(RestaurantEntity e) {
        return new RestaurantVO(
            e.getId(),
            e.getName(),
            e.getDistrict(),
            e.getBusinessArea(),
            e.getAddress(),
            e.getAveragePrice(),
            e.getCuisine(),
            e.getRating(),
            TagConverter.jsonToTags(e.getTasteTags()),
            TagConverter.jsonToTags(e.getSceneTags()),
            TagConverter.jsonToTags(e.getAvoidTags()),
            TagConverter.jsonToTags(e.getRecommendedDishes()),
            e.getDescription(),
            e.getLatitude(),
            e.getLongitude(),
            e.getCoverImage(),
            e.getSource(),
            e.getSourceNote(),
            e.getIsDemoData() != null && e.getIsDemoData() == 1,
            e.getStatus(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
