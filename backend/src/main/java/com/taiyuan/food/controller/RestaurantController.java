package com.taiyuan.food.controller;

import com.taiyuan.food.common.ApiResponse;
import com.taiyuan.food.service.RestaurantQueryService;
import com.taiyuan.food.vo.PageResultVO;
import com.taiyuan.food.vo.RestaurantVO;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class RestaurantController {
    private final RestaurantQueryService restaurantQueryService;

    public RestaurantController(RestaurantQueryService restaurantQueryService) {
        this.restaurantQueryService = restaurantQueryService;
    }

    @GetMapping("/restaurants")
    public ApiResponse<PageResultVO<RestaurantVO>> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String district,
        @RequestParam(required = false) String businessArea,
        @RequestParam(required = false) String cuisine,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String tasteTag,
        @RequestParam(required = false) String sceneTag,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(
            restaurantQueryService.list(keyword, district, businessArea, cuisine,
                maxPrice, tasteTag, sceneTag, page, pageSize)
        );
    }

    @GetMapping("/restaurants/{id}")
    public ApiResponse<RestaurantVO> detail(@PathVariable long id) {
        RestaurantVO restaurant = restaurantQueryService.findActiveById(id);
        if (restaurant == null) {
            return ApiResponse.fail(404, "餐厅不存在或已下架");
        }
        return ApiResponse.success(restaurant);
    }
}
