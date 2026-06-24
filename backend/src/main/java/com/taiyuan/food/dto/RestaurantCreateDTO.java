package com.taiyuan.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RestaurantCreateDTO {
    @NotBlank
    private String name;

    @NotBlank
    private String district;

    private String businessArea;
    private String address;

    @NotNull
    private BigDecimal averagePrice;

    @NotBlank
    private String cuisine;

    private BigDecimal rating;
    private List<String> tasteTags;
    private List<String> sceneTags;
    private List<String> avoidTags;
    private List<String> recommendedDishes;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String coverImage;
    private String sourceNote;
    private Integer isDemoData = 1;
}
