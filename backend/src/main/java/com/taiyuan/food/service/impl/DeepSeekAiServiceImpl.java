package com.taiyuan.food.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiyuan.food.config.AiProperties;
import com.taiyuan.food.dto.AiFoodIntentRequestDTO;
import com.taiyuan.food.dto.AiReasonRequestDTO;
import com.taiyuan.food.dto.AiTagSuggestionRequestDTO;
import com.taiyuan.food.service.AiService;
import com.taiyuan.food.vo.AiFoodIntentVO;
import com.taiyuan.food.vo.AiReasonVO;
import com.taiyuan.food.vo.AiTagSuggestionVO;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class DeepSeekAiServiceImpl implements AiService {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiServiceImpl.class);
    private static final List<String> TASTE_TAG_WHITELIST = List.of(
        "清淡", "微辣", "中辣", "重辣", "麻辣", "咸香", "酸甜", "鲜香", "碳水", "甜口", "油香", "下饭"
    );
    private static final List<String> SCENE_TAG_WHITELIST = List.of(
        "一人食", "学生党", "朋友聚餐", "家庭聚餐", "夜宵", "约会", "打卡", "快餐", "性价比", "多人用餐", "赶时间"
    );
    private static final List<String> AVOID_TAG_WHITELIST = List.of(
        "太辣", "油腻", "香菜", "牛羊肉", "海鲜", "甜食", "冷食", "排队久"
    );

    private final AiProperties props;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;
    private final ExecutorService executor;

    public DeepSeekAiServiceImpl(AiProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        factory.setReadTimeout(java.time.Duration.ofSeconds(props.getTimeoutSeconds()));
        this.restTemplate = new RestTemplate(factory);
        this.executor = Executors.newFixedThreadPool(2);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isEnabled() {
        return props.isEnabled() && props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    @Override
    public AiReasonVO polishReason(AiReasonRequestDTO request) {
        if (!isEnabled()) {
            return new AiReasonVO(request.localReason(), false, true, props.getModel(), 0);
        }
        long start = System.currentTimeMillis();
        try {
            String prompt = buildReasonPrompt(request);
            String response = callWithTimeout(prompt);
            JsonNode json = mapper.readTree(response);
            String reason = json.path("reason").asText(request.localReason());
            long latency = System.currentTimeMillis() - start;
            log.info("AI reason polished: model={}, latencyMs={}", props.getModel(), latency);
            return new AiReasonVO(reason, true, false, props.getModel(), latency);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("AI reason fallback: error={}, latencyMs={}", e.getClass().getSimpleName(), latency);
            return new AiReasonVO(request.localReason(), false, true, props.getModel(), latency);
        }
    }

    @Override
    public AiTagSuggestionVO suggestTags(AiTagSuggestionRequestDTO request) {
        if (!isEnabled()) {
            return new AiTagSuggestionVO(List.of(), List.of(), List.of(), 0, false, true, props.getModel(), 0);
        }
        long start = System.currentTimeMillis();
        try {
            String prompt = buildTagPrompt(request);
            String response = callWithTimeout(prompt);
            JsonNode json = mapper.readTree(response);
            List<String> taste = filterWhitelist(jsonList(json, "tasteTags"), TASTE_TAG_WHITELIST);
            List<String> scene = filterWhitelist(jsonList(json, "sceneTags"), SCENE_TAG_WHITELIST);
            List<String> avoid = filterWhitelist(jsonList(json, "avoidTags"), AVOID_TAG_WHITELIST);
            int confidence = Math.clamp(json.path("confidence").asInt(0), 0, 100);
            long latency = System.currentTimeMillis() - start;
            log.info("AI tags suggested: model={}, latencyMs={}, confidence={}", props.getModel(), latency, confidence);
            return new AiTagSuggestionVO(taste, scene, avoid, confidence, true, false, props.getModel(), latency);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("AI tags fallback: error={}, latencyMs={}", e.getClass().getSimpleName(), latency);
            return new AiTagSuggestionVO(List.of(), List.of(), List.of(), 0, false, true, props.getModel(), latency);
        }
    }

    @Override
    public AiFoodIntentVO interpretFoodIntent(AiFoodIntentRequestDTO request) {
        long start = System.currentTimeMillis();
        if (!isEnabled()) {
            return localIntent(request, props.getModel(), 0);
        }
        try {
            String prompt = buildIntentPrompt(request);
            String response = callWithTimeout(prompt);
            JsonNode json = mapper.readTree(response);
            long latency = System.currentTimeMillis() - start;
            return new AiFoodIntentVO(
                fallbackText(json.path("district").asText(), request.defaultDistrict()),
                fallbackText(json.path("businessArea").asText(), request.defaultBusinessArea()),
                parsePositivePrice(json.path("maxPrice"), request.defaultMaxPrice()),
                filterWhitelist(jsonList(json, "cuisines"), request.allowedCuisines()),
                filterWhitelist(jsonList(json, "tasteTags"), request.allowedTasteTags()),
                filterWhitelist(jsonList(json, "sceneTags"), request.allowedSceneTags()),
                filterWhitelist(jsonList(json, "avoidTags"), request.allowedAvoidTags()),
                fallbackText(json.path("summary").asText(), "已根据你的描述提取偏好"),
                true,
                false,
                props.getModel(),
                latency
            );
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("AI food intent fallback: error={}, latencyMs={}", e.getClass().getSimpleName(), latency);
            return localIntent(request, props.getModel(), latency);
        }
    }

    private String buildReasonPrompt(AiReasonRequestDTO r) throws JsonProcessingException {
        String input = mapper.writeValueAsString(Map.of(
            "restaurantName", r.restaurantName(),
            "businessArea", r.businessArea() != null ? r.businessArea() : "",
            "averagePrice", r.averagePrice(),
            "cuisine", r.cuisine(),
            "matchedTags", r.matchedTags(),
            "localReason", r.localReason()
        ));
        return buildMessages(
            "你是一个简洁的太原觅食推荐助手。基于输入的餐厅信息写20-40字中文推荐理由。不要添加输入中不存在的事实。只返回JSON: {\"reason\":\"...\"}",
            input
        );
    }

    private String buildTagPrompt(AiTagSuggestionRequestDTO r) throws JsonProcessingException {
        String input = mapper.writeValueAsString(Map.of(
            "name", r.name(),
            "cuisine", r.cuisine(),
            "averagePrice", r.averagePrice(),
            "description", r.description() != null ? r.description() : "",
            "allowedTasteTags", r.allowedTasteTags(),
            "allowedSceneTags", r.allowedSceneTags()
        ));
        return buildMessages(
            "从白名单中选择适合的标签。只返回JSON: {\"tasteTags\":[],\"sceneTags\":[],\"avoidTags\":[],\"confidence\":0}。confidence 0-100。不确定时返回空数组。",
            input
        );
    }

    private String buildIntentPrompt(AiFoodIntentRequestDTO r) throws JsonProcessingException {
        String input = mapper.writeValueAsString(Map.of(
            "userPrompt", r.prompt() != null ? r.prompt() : "",
            "defaultDistrict", r.defaultDistrict() != null ? r.defaultDistrict() : "",
            "defaultBusinessArea", r.defaultBusinessArea() != null ? r.defaultBusinessArea() : "",
            "defaultMaxPrice", r.defaultMaxPrice() != null ? r.defaultMaxPrice() : BigDecimal.valueOf(35),
            "allowedCuisines", r.allowedCuisines(),
            "allowedTasteTags", r.allowedTasteTags(),
            "allowedSceneTags", r.allowedSceneTags(),
            "allowedAvoidTags", r.allowedAvoidTags()
        ));
        return buildMessages(
            "你是太原校园美食推荐意图解析器。只从白名单里抽取条件，不要编造餐厅。只返回JSON: {\"district\":\"\",\"businessArea\":\"\",\"maxPrice\":35,\"cuisines\":[],\"tasteTags\":[],\"sceneTags\":[],\"avoidTags\":[],\"summary\":\"20字以内中文总结\"}。如果用户没说某项，使用默认值或空数组。",
            input
        );
    }

    private String buildMessages(String systemPrompt, String userInput) throws JsonProcessingException {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userInput)
        );
        return mapper.writeValueAsString(Map.of(
            "model", props.getModel(),
            "messages", messages,
            "temperature", 0.3,
            "max_tokens", 300,
            "stream", false
        ));
    }

    private String callWithTimeout(String body) {
        Future<String> future = executor.submit(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getApiKey());
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(
                props.getBaseUrl() + "/v1/chat/completions", entity, String.class);
            if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null) {
                throw new RuntimeException("AI API returned " + resp.getStatusCode());
            }
            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("AI API returned empty choices");
            }
            return choices.get(0).path("message").path("content").asText();
        });
        try {
            return future.get(props.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("AI timeout after " + props.getTimeoutSeconds() + "s");
        } catch (Exception e) {
            throw new RuntimeException("AI call failed: " + e.getMessage());
        }
    }

    private List<String> jsonList(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        for (JsonNode item : node.path(field)) {
            String text = item.asText();
            if (text != null && !text.isBlank()) result.add(text);
        }
        return result;
    }

    private List<String> filterWhitelist(List<String> values, List<String> whitelist) {
        return values.stream().filter(whitelist::contains).toList();
    }

    private AiFoodIntentVO localIntent(AiFoodIntentRequestDTO request, String model, long latencyMs) {
        String text = request.prompt() == null ? "" : request.prompt();
        String district = request.defaultDistrict();
        String businessArea = request.defaultBusinessArea();
        if (text.contains("柳巷")) businessArea = "柳巷";
        if (text.contains("万达")) businessArea = "万达广场";
        if (text.contains("亲贤")) businessArea = "亲贤街";
        if (text.contains("南站")) businessArea = "太原南站周边";
        if (text.contains("中北")) businessArea = "中北大学周边";

        BigDecimal maxPrice = request.defaultMaxPrice();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{1,3})\\s*(元|块|以内|以下)?").matcher(text);
        if (matcher.find()) {
            maxPrice = new BigDecimal(matcher.group(1));
        }

        List<String> cuisines = new ArrayList<>(matchWords(text, request.allowedCuisines()));
        List<String> taste = new ArrayList<>(matchWords(text, request.allowedTasteTags()));
        List<String> scene = new ArrayList<>(matchWords(text, request.allowedSceneTags()));
        List<String> avoid = new ArrayList<>(matchWords(text, request.allowedAvoidTags()));

        if (text.contains("辣") && taste.stream().noneMatch(tag -> tag.contains("辣"))) taste.add("微辣");
        if (text.contains("甜") && !taste.contains("甜口")) taste.add("甜口");
        if (text.contains("一个人") || text.contains("一人")) addIfAllowed(scene, "一人食", request.allowedSceneTags());
        if (text.contains("学生")) addIfAllowed(scene, "学生党", request.allowedSceneTags());
        if (text.contains("聚餐")) addIfAllowed(scene, "朋友聚餐", request.allowedSceneTags());
        if (text.contains("夜宵")) addIfAllowed(scene, "夜宵", request.allowedSceneTags());
        if (text.contains("快点") || text.contains("赶时间") || text.contains("快餐")) addIfAllowed(scene, "赶时间", request.allowedSceneTags());
        if (text.contains("便宜") || text.contains("平价") || text.contains("性价比")) {
            addIfAllowed(scene, "性价比", request.allowedSceneTags());
            if (maxPrice == null || maxPrice.compareTo(BigDecimal.valueOf(30)) > 0) maxPrice = BigDecimal.valueOf(30);
        }
        if (text.contains("不吃辣") || text.contains("不要辣") || text.contains("别辣")) {
            addIfAllowed(avoid, "太辣", request.allowedAvoidTags());
            taste.removeIf(tag -> tag.contains("辣"));
        }

        return new AiFoodIntentVO(
            district,
            businessArea,
            maxPrice,
            cuisines.stream().distinct().toList(),
            taste.stream().distinct().toList(),
            scene.stream().distinct().toList(),
            avoid.stream().distinct().toList(),
            "已用本地解析理解你的描述",
            false,
            true,
            model,
            latencyMs
        );
    }

    private List<String> matchWords(String text, List<String> whitelist) {
        if (whitelist == null) return List.of();
        return whitelist.stream()
            .filter(word -> text != null && text.contains(word))
            .distinct()
            .toList();
    }

    private void addIfAllowed(List<String> target, String value, List<String> whitelist) {
        if (whitelist != null && whitelist.contains(value) && !target.contains(value)) target.add(value);
    }

    private BigDecimal parsePositivePrice(JsonNode node, BigDecimal fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) return fallback;
        try {
            BigDecimal value = new BigDecimal(node.asText());
            return value.compareTo(BigDecimal.ZERO) > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String fallbackText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
