package com.taiyuan.food.controller;

import com.taiyuan.food.common.ApiResponse;
import com.taiyuan.food.dto.AiRecommendationRequestDTO;
import com.taiyuan.food.dto.RecommendationRequestDTO;
import com.taiyuan.food.dto.ShakeRequestDTO;
import com.taiyuan.food.service.RecommendationService;
import com.taiyuan.food.vo.AiRecommendationResultVO;
import com.taiyuan.food.vo.RecommendationResultVO;
import com.taiyuan.food.vo.ShakeResultVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/recommendations")
    public ApiResponse<RecommendationResultVO> recommend(@RequestBody RecommendationRequestDTO request) {
        return ApiResponse.success(recommendationService.recommend(request));
    }

    @PostMapping("/recommendations/ai")
    public ApiResponse<AiRecommendationResultVO> aiRecommend(@RequestBody AiRecommendationRequestDTO request) {
        return ApiResponse.success(recommendationService.aiRecommend(request));
    }

    @PostMapping("/recommendations/shake")
    public ApiResponse<ShakeResultVO> shake(@RequestBody ShakeRequestDTO request) {
        ShakeResultVO result = recommendationService.shake(request);
        if (result == null) {
            return ApiResponse.fail(404, "暂无可摇出的餐厅");
        }
        return ApiResponse.success(result);
    }
}
