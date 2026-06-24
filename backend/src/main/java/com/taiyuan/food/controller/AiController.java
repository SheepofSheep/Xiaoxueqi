package com.taiyuan.food.controller;

import com.taiyuan.food.common.ApiResponse;
import com.taiyuan.food.dto.AiTagSuggestionRequestDTO;
import com.taiyuan.food.service.AiService;
import com.taiyuan.food.vo.AiTagSuggestionVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/tag-suggestions")
    public ApiResponse<AiTagSuggestionVO> suggestTags(@RequestBody AiTagSuggestionRequestDTO request) {
        AiTagSuggestionVO result = aiService.suggestTags(request);
        if (!result.aiUsed() && result.aiFallback()) {
            return ApiResponse.fail(601, "AI 服务未启用或请求超时");
        }
        return ApiResponse.success(result);
    }
}
