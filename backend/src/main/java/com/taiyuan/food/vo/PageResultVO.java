package com.taiyuan.food.vo;

import java.util.List;

public record PageResultVO<T>(
    List<T> list,
    int page,
    int pageSize,
    long total
) {
}
