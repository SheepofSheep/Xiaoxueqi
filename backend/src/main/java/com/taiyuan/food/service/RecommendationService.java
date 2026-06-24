package com.taiyuan.food.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiyuan.food.config.AiProperties;
import com.taiyuan.food.dto.AiFoodIntentRequestDTO;
import com.taiyuan.food.dto.AiRecommendationRequestDTO;
import com.taiyuan.food.dto.RecommendationRequestDTO;
import com.taiyuan.food.dto.ShakeRequestDTO;
import com.taiyuan.food.vo.AiFoodIntentVO;
import com.taiyuan.food.vo.AiRecommendationResultVO;
import com.taiyuan.food.vo.PageResultVO;
import com.taiyuan.food.vo.RecommendationItemVO;
import com.taiyuan.food.vo.RecommendationResultVO;
import com.taiyuan.food.vo.RestaurantVO;
import com.taiyuan.food.vo.ShakeResultVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RecommendationService {
    private static final BigDecimal BUDGET_RELAX_STEP = BigDecimal.TEN;
    private static final BigDecimal BUDGET_RELAX_MAX = BigDecimal.valueOf(20);
    private static final int MIN_FULL_MATCH_RESULTS = 3;

    private static final List<String> ALLOWED_CUISINES = List.of(
        "面食", "山西面食", "麻辣烫", "火锅", "烧烤", "快餐", "饮品", "小吃", "川菜", "黄焖鸡"
    );
    private static final List<String> ALLOWED_TASTE_TAGS = List.of(
        "清淡", "微辣", "中辣", "重辣", "麻辣", "咸香", "酸甜", "鲜香", "碳水", "甜口", "油香", "下饭"
    );
    private static final List<String> ALLOWED_SCENE_TAGS = List.of(
        "一人食", "学生党", "朋友聚餐", "家庭聚餐", "夜宵", "约会", "打卡", "快餐", "性价比", "多人用餐", "赶时间"
    );
    private static final List<String> ALLOWED_AVOID_TAGS = List.of(
        "太辣", "油腻", "香菜", "牛羊肉", "海鲜", "甜食", "冷食", "排队久"
    );

    private final RestaurantQueryService restaurantQueryService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AiService aiService;
    private final AiProperties aiProperties;
    private final TransactionTemplate transactionTemplate;

    public RecommendationService(
        RestaurantQueryService restaurantQueryService,
        JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper,
        AiService aiService,
        AiProperties aiProperties,
        TransactionTemplate transactionTemplate
    ) {
        this.restaurantQueryService = restaurantQueryService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.aiService = aiService;
        this.aiProperties = aiProperties;
        this.transactionTemplate = transactionTemplate;
    }

    public RecommendationResultVO recommend(RecommendationRequestDTO request) {
        RecommendationRequestDTO safeRequest = normalizeRequest(request);
        int limit = safeRequest.limit() == null || safeRequest.limit() < 1 ? 6 : Math.min(safeRequest.limit(), 20);
        RecommendationSelection selection = buildRecommendationSelection(safeRequest, limit);
        boolean anyAiUsed = false;
        List<RecommendationItemVO> polished = new ArrayList<>();
        for (RecommendationItemVO item : selection.items()) {
            RecommendationItemVO p = polishWithAi(item, safeRequest);
            if (p.aiUsed()) anyAiUsed = true;
            polished.add(p);
        }
        RecommendationResultVO result = new RecommendationResultVO(
            polished,
            selection.relaxed() || polished.isEmpty(),
            polished.isEmpty() ? List.of("当前条件下暂无匹配餐厅，可放宽菜系、预算或忌口条件") : selection.relaxedRules(),
            true,
            anyAiUsed,
            !anyAiUsed
        );
        saveHistoryTx(safeRequest, result, "NORMAL");
        return result;
    }

    public AiRecommendationResultVO aiRecommend(AiRecommendationRequestDTO request) {
        AiRecommendationRequestDTO safeRequest = request == null
            ? new AiRecommendationRequestDTO("", "尖草坪区", "中北大学周边", BigDecimal.valueOf(35), 8)
            : request;
        BigDecimal defaultMaxPrice = safeRequest.maxPrice() == null ? BigDecimal.valueOf(35) : safeRequest.maxPrice();
        AiFoodIntentVO intent = aiService.interpretFoodIntent(new AiFoodIntentRequestDTO(
            safeRequest.prompt(),
            blankToDefault(safeRequest.district(), "尖草坪区"),
            blankToDefault(safeRequest.businessArea(), "中北大学周边"),
            defaultMaxPrice,
            ALLOWED_CUISINES,
            ALLOWED_TASTE_TAGS,
            ALLOWED_SCENE_TAGS,
            ALLOWED_AVOID_TAGS
        ));
        int limit = safeRequest.limit() == null || safeRequest.limit() < 1 ? 8 : Math.min(safeRequest.limit(), 20);
        RecommendationRequestDTO interpreted = new RecommendationRequestDTO(
            intent.district(),
            intent.businessArea(),
            intent.maxPrice() == null ? defaultMaxPrice : intent.maxPrice(),
            intent.cuisines(),
            intent.tasteTags(),
            intent.sceneTags(),
            intent.avoidTags(),
            null,
            limit,
            null
        );
        RecommendationResultVO recommendation = recommend(interpreted);
        return new AiRecommendationResultVO(recommendation, interpreted, intent);
    }

    private RecommendationItemVO polishWithAi(RecommendationItemVO item, RecommendationRequestDTO request) {
        if (!aiProperties.isReasonPolishEnabled() || !aiService.isEnabled()) return item;
        try {
            com.taiyuan.food.dto.AiReasonRequestDTO aiReq = new com.taiyuan.food.dto.AiReasonRequestDTO(
                item.restaurant().name(),
                item.restaurant().businessArea(),
                item.restaurant().averagePrice() != null ? item.restaurant().averagePrice().intValue() : 0,
                item.restaurant().cuisine(),
                item.matchedTags(),
                item.reason()
            );
            com.taiyuan.food.vo.AiReasonVO aiResp = aiService.polishReason(aiReq);
            return new RecommendationItemVO(
                item.restaurant(),
                item.matchScore(),
                item.matchedTags(),
                aiResp.reason(),
                aiResp.aiUsed(),
                aiResp.aiFallback(),
                aiResp.aiModel(),
                aiResp.aiLatencyMs()
            );
        } catch (Exception e) {
            return item;
        }
    }

    public ShakeResultVO shake(ShakeRequestDTO request) {
        ShakeRequestDTO safeRequest = request == null
            ? new ShakeRequestDTO(null, null, null, null, null, null)
            : request;
        RecommendationRequestDTO recommendationRequest = new RecommendationRequestDTO(
            safeRequest.district(),
            safeRequest.businessArea(),
            safeRequest.maxPrice(),
            null,
            null,
            null,
            safeRequest.avoidTags(),
            1,
            20,
            safeRequest.excludeRestaurantIds()
        );
        List<RecommendationItemVO> candidates = buildRecommendationItems(recommendationRequest, 20).stream()
            .filter(item -> withinBudget(item.restaurant(), safeRequest.maxPrice()))
            .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        RecommendationItemVO picked = pickWeighted(candidates);
        String clientTrigger = normalizeClientTrigger(safeRequest.clientTrigger());
        ShakeResultVO result = new ShakeResultVO(
            picked.restaurant(),
            picked.matchScore(),
            "摇到了：" + picked.reason(),
            clientTrigger
        );
        saveHistoryTx(safeRequest, result, "DEVICE_MOTION".equals(clientTrigger) ? "SHAKE_DEVICE" : "SHAKE_BUTTON");
        return result;
    }

    private RecommendationSelection buildRecommendationSelection(RecommendationRequestDTO request, int limit) {
        List<RecommendationItemVO> scored = scoreCandidates(request, BUDGET_RELAX_MAX);
        List<RecommendationItemVO> strict = selectBudgetWindow(scored, request.maxPrice(), BigDecimal.ZERO, limit);
        if (request.maxPrice() == null || strict.size() >= Math.min(limit, MIN_FULL_MATCH_RESULTS)) {
            return new RecommendationSelection(strict, false, List.of());
        }

        List<RecommendationItemVO> relaxedTen = selectBudgetWindow(scored, request.maxPrice(), BUDGET_RELAX_STEP, limit);
        if (relaxedTen.size() >= Math.min(limit, MIN_FULL_MATCH_RESULTS) || relaxedTen.size() > strict.size()) {
            return new RecommendationSelection(relaxedTen, true, List.of("预算放宽 10 元"));
        }

        List<RecommendationItemVO> relaxedTwenty = selectBudgetWindow(scored, request.maxPrice(), BUDGET_RELAX_MAX, limit);
        if (!relaxedTwenty.isEmpty()) {
            return new RecommendationSelection(relaxedTwenty, true, List.of("预算放宽 20 元"));
        }

        return new RecommendationSelection(strict, false, List.of());
    }

    private List<RecommendationItemVO> buildRecommendationItems(RecommendationRequestDTO request, int limit) {
        return buildRecommendationItems(request, limit, BUDGET_RELAX_MAX);
    }

    private List<RecommendationItemVO> buildRecommendationItems(
        RecommendationRequestDTO request,
        int limit,
        BigDecimal budgetRelaxAmount
    ) {
        return scoreCandidates(request, budgetRelaxAmount).stream()
            .limit(limit)
            .toList();
    }

    private List<RecommendationItemVO> scoreCandidates(RecommendationRequestDTO request, BigDecimal budgetRelaxAmount) {
        PageResultVO<RestaurantVO> page = restaurantQueryService.list(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            1,
            1000
        );

        Set<Long> excludedIds = new HashSet<>(safeLongList(request.excludeRestaurantIds()));
        List<String> avoidTags = safeStringList(request.avoidTags());
        return page.list().stream()
            .filter(restaurant -> !excludedIds.contains(restaurant.id()))
            .filter(restaurant -> !hasAny(restaurant.avoidTags(), avoidTags))
            .filter(restaurant -> withinBudgetRelaxation(restaurant, request.maxPrice(), budgetRelaxAmount))
            .filter(restaurant -> matchesRequestedCuisine(restaurant.cuisine(), request.cuisines()))
            .map(restaurant -> score(restaurant, request))
            .filter(item -> item.matchScore() > 20)
            .sorted(Comparator.comparingInt(RecommendationItemVO::matchScore).reversed()
                .thenComparing(item -> item.restaurant().rating(), Comparator.reverseOrder())
                .thenComparing(item -> item.restaurant().id()))
            .toList();
    }

    private List<RecommendationItemVO> selectBudgetWindow(
        List<RecommendationItemVO> scored,
        BigDecimal maxPrice,
        BigDecimal budgetRelaxAmount,
        int limit
    ) {
        return scored.stream()
            .filter(item -> withinBudgetRelaxation(item.restaurant(), maxPrice, budgetRelaxAmount))
            .limit(limit)
            .toList();
    }

    private RecommendationItemVO score(RestaurantVO restaurant, RecommendationRequestDTO request) {
        List<String> matchedTags = new ArrayList<>();

        int areaScore = areaScore(restaurant, request, matchedTags);
        int budgetScore = budgetScore(restaurant, request.maxPrice(), matchedTags);
        int cuisineScore = cuisineScore(restaurant.cuisine(), request.cuisines(), matchedTags);
        int tasteScore = tagScore(restaurant.tasteTags(), request.tasteTags(), matchedTags);
        int sceneScore = tagScore(restaurant.sceneTags(), request.sceneTags(), matchedTags);
        int ratingScore = ratingScore(restaurant.rating());

        int score = clampScore(
            Math.round(
                areaScore * 0.25f
                    + budgetScore * 0.20f
                    + cuisineScore * 0.20f
                    + tasteScore * 0.15f
                    + sceneScore * 0.10f
                    + ratingScore * 0.10f
            )
        );
        return new RecommendationItemVO(
            restaurant,
            score,
            dedupe(matchedTags),
            buildReason(restaurant, matchedTags, request.maxPrice()),
            false,
            true,
            null,
            0
        );
    }

    private int areaScore(RestaurantVO restaurant, RecommendationRequestDTO request, List<String> matchedTags) {
        if (matches(request.businessArea(), restaurant.businessArea())) {
            matchedTags.add("商圈匹配");
            return 100;
        }
        if (matches(request.district(), restaurant.district())) {
            matchedTags.add("区域匹配");
            return 80;
        }
        if (isBlank(request.businessArea()) && isBlank(request.district())) {
            return 60;
        }
        return 40;
    }

    private int budgetScore(RestaurantVO restaurant, BigDecimal maxPrice, List<String> matchedTags) {
        if (maxPrice == null) {
            return 60;
        }
        if (restaurant.averagePrice() == null) {
            return 50;
        }
        BigDecimal overBudget = restaurant.averagePrice().subtract(maxPrice);
        if (overBudget.compareTo(BigDecimal.ZERO) <= 0) {
            matchedTags.add("预算匹配");
            return 100;
        }
        if (overBudget.compareTo(BUDGET_RELAX_STEP) <= 0) {
            matchedTags.add("略超预算");
            return 75;
        }
        if (overBudget.compareTo(BUDGET_RELAX_MAX) <= 0) {
            matchedTags.add("预算放宽");
            return 45;
        }
        return 10;
    }

    private int cuisineScore(String cuisine, List<String> requestedCuisines, List<String> matchedTags) {
        List<String> safeCuisines = safeStringList(requestedCuisines);
        if (safeCuisines.isEmpty()) {
            return 60;
        }
        for (String requested : safeCuisines) {
            if (cuisineMatches(cuisine, requested)) {
                matchedTags.add("菜系匹配");
                return 100;
            }
        }
        for (String requested : safeCuisines) {
            if (relatedCuisine(cuisine, requested)) {
                matchedTags.add("相近菜系");
                return 70;
            }
        }
        return 20;
    }

    private int tagScore(List<String> restaurantTags, List<String> requestedTags, List<String> matchedTags) {
        List<String> safeRequestedTags = safeStringList(requestedTags);
        if (safeRequestedTags.isEmpty()) {
            return 60;
        }
        List<String> hits = intersection(restaurantTags, safeRequestedTags);
        matchedTags.addAll(hits);
        return Math.round(hits.size() * 100f / safeRequestedTags.size());
    }

    private int ratingScore(BigDecimal rating) {
        if (rating == null) {
            return 60;
        }
        return rating.divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();
    }

    private String buildReason(RestaurantVO restaurant, List<String> matchedTags, BigDecimal maxPrice) {
        List<String> parts = new ArrayList<>();
        if (!matchedTags.isEmpty()) {
            parts.add(String.join("、", matchedTags.subList(0, Math.min(3, matchedTags.size()))));
        }
        if (maxPrice != null && restaurant.averagePrice() != null
            && restaurant.averagePrice().compareTo(maxPrice) <= 0) {
            parts.add("人均 " + restaurant.averagePrice().stripTrailingZeros().toPlainString() + " 元在预算内");
        }
        if (restaurant.recommendedDishes() != null && !restaurant.recommendedDishes().isEmpty()) {
            parts.add("可试试" + restaurant.recommendedDishes().getFirst());
        }
        if (parts.isEmpty()) {
            return "这家店与当前条件有一定匹配，适合作为备选。";
        }
        return String.join("，", parts) + "。";
    }

    private boolean matches(String expected, String actual) {
        return expected != null && !expected.isBlank() && expected.trim().equals(actual);
    }

    private boolean cuisineMatches(String actual, String expected) {
        if (isBlank(actual) || isBlank(expected)) {
            return false;
        }
        String safeActual = actual.trim();
        String safeExpected = expected.trim();
        return safeActual.equals(safeExpected)
            || safeActual.contains(safeExpected)
            || safeExpected.contains(safeActual);
    }

    private boolean matchesRequestedCuisine(String actual, List<String> requestedCuisines) {
        List<String> safeCuisines = safeStringList(requestedCuisines);
        if (safeCuisines.isEmpty()) {
            return true;
        }
        return safeCuisines.stream().anyMatch(requested -> cuisineMatches(actual, requested) || relatedCuisine(actual, requested));
    }

    private boolean relatedCuisine(String actual, String expected) {
        if (isBlank(actual) || isBlank(expected)) {
            return false;
        }
        Set<String> actualGroup = cuisineGroup(actual);
        Set<String> expectedGroup = cuisineGroup(expected);
        return !actualGroup.isEmpty() && actualGroup.stream().anyMatch(expectedGroup::contains);
    }

    private Set<String> cuisineGroup(String cuisine) {
        if (cuisineMatches(cuisine, "山西面食") || cuisineMatches(cuisine, "面食") || cuisineMatches(cuisine, "快餐")) {
            return Set.of("面食快餐");
        }
        if (cuisineMatches(cuisine, "麻辣烫") || cuisineMatches(cuisine, "火锅") || cuisineMatches(cuisine, "串串")) {
            return Set.of("热辣锅物");
        }
        if (cuisineMatches(cuisine, "饮品") || cuisineMatches(cuisine, "奶茶") || cuisineMatches(cuisine, "甜品")) {
            return Set.of("饮品甜品");
        }
        if (cuisineMatches(cuisine, "烧烤") || cuisineMatches(cuisine, "夜宵") || cuisineMatches(cuisine, "小吃")) {
            return Set.of("夜宵小吃");
        }
        return Set.of(cuisine.trim());
    }

    private List<String> intersection(List<String> left, List<String> right) {
        Set<String> rightSet = new HashSet<>(safeStringList(right));
        return safeStringList(left).stream()
            .filter(rightSet::contains)
            .distinct()
            .toList();
    }

    private boolean hasAny(List<String> values, List<String> targets) {
        return !intersection(values, targets).isEmpty();
    }

    private boolean withinBudget(RestaurantVO restaurant, BigDecimal maxPrice) {
        if (maxPrice == null) {
            return true;
        }
        return restaurant.averagePrice() != null && restaurant.averagePrice().compareTo(maxPrice) <= 0;
    }

    private boolean withinBudgetRelaxation(RestaurantVO restaurant, BigDecimal maxPrice, BigDecimal relaxAmount) {
        if (maxPrice == null) {
            return true;
        }
        if (restaurant.averagePrice() == null) {
            return false;
        }
        BigDecimal safeRelaxAmount = relaxAmount == null ? BigDecimal.ZERO : relaxAmount.max(BigDecimal.ZERO);
        return restaurant.averagePrice().compareTo(maxPrice.add(safeRelaxAmount)) <= 0;
    }

    private RecommendationItemVO pickWeighted(List<RecommendationItemVO> candidates) {
        int totalWeight = candidates.stream()
            .mapToInt(item -> Math.max(item.matchScore(), 10))
            .sum();
        int cursor = ThreadLocalRandom.current().nextInt(totalWeight);
        for (RecommendationItemVO item : candidates) {
            cursor -= Math.max(item.matchScore(), 10);
            if (cursor < 0) {
                return item;
            }
        }
        return candidates.getFirst();
    }

    private String normalizeClientTrigger(String clientTrigger) {
        return "DEVICE_MOTION".equals(clientTrigger) ? "DEVICE_MOTION" : "BUTTON_FALLBACK";
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private RecommendationRequestDTO normalizeRequest(RecommendationRequestDTO request) {
        return request == null
            ? new RecommendationRequestDTO(null, null, null, null, null, null, null, null, 6, null)
            : request;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private List<String> dedupe(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(safeStringList(values)));
    }

    private List<String> safeStringList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .toList();
    }

    private List<Long> safeLongList(List<Long> values) {
        return values == null ? List.of() : values;
    }

    private void saveHistoryTx(Object request, Object result, String triggerType) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String resultJson = objectMapper.writeValueAsString(result);
            transactionTemplate.executeWithoutResult(status ->
                jdbcTemplate.update(
                    "INSERT INTO recommendation_history (request_json, result_json, trigger_type) VALUES (?, ?, ?)",
                    requestJson,
                    resultJson,
                    triggerType
                )
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("推荐历史序列化失败", ex);
        }
    }

    private record RecommendationSelection(
        List<RecommendationItemVO> items,
        boolean relaxed,
        List<String> relaxedRules
    ) {
    }
}
