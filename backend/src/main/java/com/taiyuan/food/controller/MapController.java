package com.taiyuan.food.controller;

import com.taiyuan.food.common.ApiResponse;
import com.taiyuan.food.service.AmapService;
import com.taiyuan.food.vo.NearbyResultVO;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MapController {
    private final AmapService amapService;

    public MapController(AmapService amapService) {
        this.amapService = amapService;
    }

    @GetMapping("/map/nearby")
    public ApiResponse<NearbyResultVO> nearby(
        @RequestParam BigDecimal longitude,
        @RequestParam BigDecimal latitude,
        @RequestParam(defaultValue = "3000") int radius,
        @RequestParam(defaultValue = "餐饮服务") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        NearbyResultVO result = amapService.searchNearby(longitude, latitude, radius, page, pageSize);
        if (!result.list().isEmpty()) {
            return ApiResponse.success(result);
        }
        if (result.notice().contains("未配置") || result.notice().contains("不可用")) {
            return ApiResponse.fail(602, result.notice());
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/map/regeo")
    public ApiResponse<Map<String, String>> regeo(
        @RequestParam BigDecimal longitude,
        @RequestParam BigDecimal latitude
    ) {
        return ApiResponse.success(amapService.regeo(longitude, latitude));
    }
}
