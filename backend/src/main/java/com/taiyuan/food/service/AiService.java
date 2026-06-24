package com.taiyuan.food.service;

import com.taiyuan.food.vo.AiReasonVO;
import com.taiyuan.food.vo.AiTagSuggestionVO;
import com.taiyuan.food.vo.AiFoodIntentVO;
import com.taiyuan.food.dto.AiFoodIntentRequestDTO;
import com.taiyuan.food.dto.AiReasonRequestDTO;
import com.taiyuan.food.dto.AiTagSuggestionRequestDTO;

public interface AiService {
    AiReasonVO polishReason(AiReasonRequestDTO request);
    AiTagSuggestionVO suggestTags(AiTagSuggestionRequestDTO request);
    AiFoodIntentVO interpretFoodIntent(AiFoodIntentRequestDTO request);
    boolean isEnabled();
}
