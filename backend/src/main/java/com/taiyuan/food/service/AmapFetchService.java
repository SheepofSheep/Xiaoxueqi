package com.taiyuan.food.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiyuan.food.common.TagConverter;
import com.taiyuan.food.dto.AiTagSuggestionRequestDTO;
import com.taiyuan.food.entity.RestaurantEntity;
import com.taiyuan.food.mapper.RestaurantMapper;
import com.taiyuan.food.vo.AiTagSuggestionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AmapFetchService {
    private static final Logger log = LoggerFactory.getLogger(AmapFetchService.class);
    private static final List<String> TASTE_WHITELIST = List.of(
        "清淡","微辣","中辣","重辣","麻辣","咸香","酸甜","鲜香","碳水","甜口","油香","下饭");
    private static final List<String> SCENE_WHITELIST = List.of(
        "一人食","学生党","朋友聚餐","家庭聚餐","夜宵","约会","打卡","快餐","性价比","多人用餐","赶时间");

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final RestaurantMapper restaurantMapper;
    private final AiService aiService;

    @Value("${app.amap.web-service-key:}")
    private String amapKey;

    public AmapFetchService(ObjectMapper mapper, RestaurantMapper restaurantMapper, AiService aiService) {
        this.restTemplate = new RestTemplate();
        this.mapper = mapper;
        this.restaurantMapper = restaurantMapper;
        this.aiService = aiService;
    }

    @Transactional
    public int fetchAndSave(double lng, double lat, int radius, String businessArea, String district, int pageLimit) {
        if (amapKey == null || amapKey.isBlank()) {
            log.warn("AMAP key not configured");
            return 0;
        }
        int saved = 0;
        int page = 1;
        while (page <= Math.min(pageLimit, 5)) {
            String url = String.format(
                "https://restapi.amap.com/v3/place/around?key=%s&location=%s,%s&radius=%d&types=050000&offset=20&page=%d&extensions=all",
                amapKey, lng, lat, radius, page);
            try {
                String body = restTemplate.getForObject(url, String.class);
                JsonNode root = mapper.readTree(body);
                if (root.path("status").asInt() != 1) break;
                JsonNode pois = root.path("pois");
                if (pois.isEmpty()) break;
                for (JsonNode poi : pois) {
                    try {
                        saved += saveOne(poi, businessArea, district);
                    } catch (Exception e) {
                        log.warn("Skip poi {}: {}", poi.path("name").asText(), e.getMessage());
                    }
                }
                int total = Integer.parseInt(root.path("count").asText("0"));
                if (page * 20 >= total) break;
            } catch (Exception e) {
                log.error("Amap fetch page {} failed: {}", page, e.getMessage());
                break;
            }
            page++;
        }
        log.info("Amap fetch done: saved={} for area={}", saved, businessArea);
        return saved;
    }

    private int saveOne(JsonNode poi, String businessArea, String district) {
        String name = poi.path("name").asText();
        String address = poi.path("address").asText();
        if (name.isBlank()) return 0;

        // Check duplicate
        var existing = restaurantMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RestaurantEntity>()
                .eq(RestaurantEntity::getName, name)
                .eq(RestaurantEntity::getAddress, address));
        if (!existing.isEmpty()) return 0;

        BigDecimal avgPrice = parsePrice(poi.path("biz_ext").path("cost").asText());
        String cuisine = mapCuisine(poi.path("type").asText());
        BigDecimal rating = parseRating(poi);

        // AI tag suggestion
        List<String> tasteTags = List.of();
        List<String> sceneTags = List.of();
        List<String> avoidTags = List.of();
        if (aiService.isEnabled()) {
            try {
                var aiReq = new AiTagSuggestionRequestDTO(
                    name, cuisine, avgPrice != null ? avgPrice.intValue() : 25,
                    "", TASTE_WHITELIST, SCENE_WHITELIST);
                AiTagSuggestionVO aiResp = aiService.suggestTags(aiReq);
                tasteTags = aiResp.tasteTags();
                sceneTags = aiResp.sceneTags();
                avoidTags = aiResp.avoidTags();
            } catch (Exception e) {
                log.debug("AI tag skip for {}: {}", name, e.getMessage());
            }
        }

        RestaurantEntity entity = new RestaurantEntity();
        entity.setName(name);
        entity.setDistrict(district);
        entity.setBusinessArea(businessArea);
        entity.setAddress(address);
        entity.setAveragePrice(avgPrice != null ? avgPrice : BigDecimal.valueOf(25));
        entity.setCuisine(cuisine);
        entity.setRating(rating);
        entity.setTasteTags(TagConverter.tagsToJson(tasteTags));
        entity.setSceneTags(TagConverter.tagsToJson(sceneTags));
        entity.setAvoidTags(TagConverter.tagsToJson(avoidTags));
        entity.setRecommendedDishes(TagConverter.tagsToJson(List.of()));
        // Extract cover photo
        String photoUrl = extractFirstPhoto(poi);
        entity.setCoverImage(photoUrl);

        entity.setDescription(poi.path("biz_ext").path("opentime").asText(""));
        // Amap returns "lng,lat" — index 0=lng, index 1=lat
        String[] locationParts = poi.path("location").asText("").split(",");
        BigDecimal longitude = parseBigDecimal(locationParts, 0);
        BigDecimal latitude = parseBigDecimal(locationParts, 1);
        if (!isValidTaiyuanCoord(latitude, longitude)) {
            log.warn("Skip poi {} because location is invalid: {}", name, poi.path("location").asText(""));
            return 0;
        }
        entity.setLongitude(longitude);
        entity.setLatitude(latitude);
        entity.setSource("AMAP_REALTIME");
        entity.setSourceNote("高德实时搜索自动入库");
        entity.setIsDemoData(0);
        entity.setStatus(1);

        restaurantMapper.insert(entity);
        return 1;
    }

    private BigDecimal parsePrice(String cost) {
        if (cost == null || cost.isBlank()) return null;
        try {
            return new BigDecimal(cost.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseRating(JsonNode poi) {
        String rating = poi.path("biz_ext").path("rating").asText();
        if (rating.isBlank()) rating = poi.path("deep_info").path("rating").asText();
        if (rating.isBlank()) return BigDecimal.valueOf(3.0);
        try {
            BigDecimal r = new BigDecimal(rating.trim());
            return r.compareTo(BigDecimal.valueOf(5)) > 0 ? BigDecimal.valueOf(5) : r;
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(3.0);
        }
    }

    private String mapCuisine(String type) {
        if (type.contains("面") || type.contains("粉")) return "面食";
        if (type.contains("火锅")) return "火锅";
        if (type.contains("烧烤") || type.contains("烤")) return "烧烤";
        if (type.contains("饮品") || type.contains("茶") || type.contains("咖啡")) return "饮品";
        if (type.contains("小吃") || type.contains("快餐")) return "小吃";
        if (type.contains("麻辣烫") || type.contains("川菜")) return "麻辣烫";
        if (type.contains("黄焖鸡") || type.contains("米饭")) return "快餐";
        if (type.contains("饺子") || type.contains("面食")) return "面食";
        return "快餐";
    }

    private String extractFirstPhoto(JsonNode poi) {
        // Try top-level photos array
        JsonNode photos = poi.path("photos");
        if (photos.isArray() && !photos.isEmpty()) {
            String url = photos.get(0).path("url").asText();
            if (!url.isBlank()) return ensureHttps(url);
        }
        // Try deep_info photos
        JsonNode deepPhotos = poi.path("deep_info").path("photos");
        if (deepPhotos.isArray() && !deepPhotos.isEmpty()) {
            String url = deepPhotos.get(0).path("url").asText();
            if (!url.isBlank()) return ensureHttps(url);
        }
        return null;
    }

    private String ensureHttps(String url) {
        if (url.startsWith("http://")) return url.replace("http://", "https://");
        return url;
    }

    private BigDecimal parseBigDecimal(String[] parts, int index) {
        if (parts.length > index) {
            try { return new BigDecimal(parts[index]); } catch (NumberFormatException e) {}
        }
        return null;
    }

    private boolean isValidTaiyuanCoord(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) return false;
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        return lat >= 35 && lat <= 41 && lng >= 110 && lng <= 115;
    }
}
