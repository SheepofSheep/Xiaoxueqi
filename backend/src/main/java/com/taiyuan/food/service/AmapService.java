package com.taiyuan.food.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiyuan.food.vo.NearbyResultVO;
import com.taiyuan.food.vo.NearbyRestaurantVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AmapService {
    private static final Logger log = LoggerFactory.getLogger(AmapService.class);
    private static final String AMAP_NEARBY_URL =
        "https://restapi.amap.com/v3/place/around?key={key}&location={lng},{lat}&radius={radius}&types=050000&offset={pageSize}&page={page}&extensions=all";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.amap.web-service-key:}")
    private String amapKey;

    public AmapService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public NearbyResultVO searchNearby(BigDecimal longitude, BigDecimal latitude,
                                        Integer radius, Integer page, Integer pageSize) {
        if (amapKey == null || amapKey.isBlank()) {
            log.warn("AMAP_WEB_SERVICE_KEY not configured");
            return new NearbyResultVO(List.of(), 1, 10, 0,
                "地图服务未配置，你仍可使用精选推荐和摇一摇。");
        }

        int safeRadius = radius == null || radius < 100 ? 3000 : Math.min(radius, 5000);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 25);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                AMAP_NEARBY_URL,
                String.class,
                amapKey,
                longitude.stripTrailingZeros().toPlainString(),
                latitude.stripTrailingZeros().toPlainString(),
                safeRadius,
                safePageSize,
                safePage
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return failResult();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            int status = root.path("status").asInt(0);
            if (status != 1) {
                String info = root.path("info").asText("unknown error");
                log.warn("Amap API error: status={}, info={}", status, info);
                return failResult();
            }

            int total = Integer.parseInt(root.path("count").asText("0"));
            List<NearbyRestaurantVO> list = new ArrayList<>();
            for (JsonNode poi : root.path("pois")) {
                String photoUrl = extractPhoto(poi);
                BigDecimal rating = parseRating(poi);
                list.add(new NearbyRestaurantVO(
                    poi.path("id").asText(),
                    poi.path("name").asText(),
                    poi.path("address").asText(),
                    Integer.parseInt(poi.path("distance").asText("0")),
                    poi.path("type").asText(),
                    parseBigDecimal(poi.path("location").asText("").split(","), 0),
                    parseBigDecimal(poi.path("location").asText("").split(","), 1),
                    "AMAP_REALTIME",
                    false,
                    photoUrl,
                    rating
                ));
            }

            return new NearbyResultVO(list, safePage, safePageSize, total,
                "地图实时结果，仅供位置参考，未参与本站口味标签评分。");

        } catch (RestClientException e) {
            log.error("Amap API network error", e);
            return failResult();
        } catch (Exception e) {
            log.error("Amap API unexpected error", e);
            return failResult();
        }
    }

    public Map<String, String> regeo(BigDecimal longitude, BigDecimal latitude) {
        if (amapKey == null || amapKey.isBlank()) return Map.of("district", "太原", "address", "定位中");
        try {
            String url = String.format("https://restapi.amap.com/v3/geocode/regeo?key=%s&location=%s,%s&extensions=base",
                amapKey, longitude.stripTrailingZeros().toPlainString(), latitude.stripTrailingZeros().toPlainString());
            String body = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body);
            if (root.path("status").asInt() == 1) {
                JsonNode comp = root.path("regeocode").path("addressComponent");
                return Map.of(
                    "district", comp.path("district").asText("太原"),
                    "township", comp.path("township").asText(""),
                    "address", root.path("regeocode").path("formatted_address").asText("太原")
                );
            }
        } catch (Exception e) { log.warn("Regeo failed: {}", e.getMessage()); }
        return Map.of("district", "太原", "address", "定位中");
    }

    private NearbyResultVO failResult() {
        return new NearbyResultVO(List.of(), 1, 10, 0,
            "附近搜索暂时不可用，你仍可使用精选推荐和摇一摇。");
    }

    private String extractPhoto(JsonNode poi) {
        JsonNode photos = poi.path("photos");
        if (photos.isArray() && !photos.isEmpty()) {
            String url = photos.get(0).path("url").asText();
            if (!url.isBlank()) return url.replace("http://", "https://");
        }
        return null;
    }

    private BigDecimal parseRating(JsonNode poi) {
        String r = poi.path("biz_ext").path("rating").asText();
        if (r.isBlank()) r = poi.path("deep_info").path("rating").asText();
        if (r.isBlank()) return null;
        try {
            BigDecimal val = new BigDecimal(r.trim());
            return val.compareTo(BigDecimal.valueOf(5)) > 0 ? BigDecimal.valueOf(5) : val;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String[] parts, int index) {
        if (parts.length > index) {
            try {
                return new BigDecimal(parts[index]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
