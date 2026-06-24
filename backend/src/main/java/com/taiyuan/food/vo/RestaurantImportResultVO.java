package com.taiyuan.food.vo;

import java.util.List;

public record RestaurantImportResultVO(
    int successCount,
    int createdCount,
    int updatedCount,
    int failureCount,
    List<CsvImportErrorVO> errors
) {
}
