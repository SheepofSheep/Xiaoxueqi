package com.taiyuan.food.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite:file:shake-recommendation-test?mode=memory&cache=shared",
    "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class ShakeRecommendationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM recommendation_history WHERE request_json LIKE '%SHAKE_TEST%'");
        jdbcTemplate.update("DELETE FROM restaurant WHERE name LIKE '摇一摇测试%'");
        insertRestaurant("摇一摇测试面馆", 22, "山西面食", "[\"咸香\",\"碳水\"]", "[\"一人食\"]", "[\"太辣\"]");
        insertRestaurant("摇一摇测试烧烤", 45, "烧烤", "[\"重辣\",\"油香\"]", "[\"夜宵\"]", "[\"清淡\"]");
    }

    @Test
    void shakesRestaurantWithButtonFallbackTrigger() throws Exception {
        String body = """
            {
              "district": "尖草坪区",
              "businessArea": "中北大学周边",
              "maxPrice": 35,
              "clientTrigger": "BUTTON_FALLBACK"
            }
            """;

        mockMvc.perform(post("/api/recommendations/shake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.restaurant.name").value("摇一摇测试面馆"))
            .andExpect(jsonPath("$.data.matchScore").exists())
            .andExpect(jsonPath("$.data.reason").exists())
            .andExpect(jsonPath("$.data.clientTrigger").value("BUTTON_FALLBACK"));
    }

    @Test
    void recordsDeviceMotionShakeHistory() throws Exception {
        String body = """
            {
              "district": "尖草坪区",
              "businessArea": "中北大学周边",
              "maxPrice": 35,
              "clientTrigger": "DEVICE_MOTION"
            }
            """;

        mockMvc.perform(post("/api/recommendations/shake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.clientTrigger").value("DEVICE_MOTION"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM recommendation_history WHERE trigger_type = 'SHAKE_DEVICE'",
            Integer.class
        );

        org.assertj.core.api.Assertions.assertThat(count).isNotNull().isGreaterThanOrEqualTo(1);
    }

    @Test
    void returns404WhenShakeHasNoCandidate() throws Exception {
        Long noodleId = jdbcTemplate.queryForObject(
            "SELECT id FROM restaurant WHERE name = '摇一摇测试面馆'",
            Long.class
        );
        String body = """
            {
              "district": "尖草坪区",
              "businessArea": "中北大学周边",
              "maxPrice": 35,
              "avoidTags": ["清淡"],
              "excludeRestaurantIds": [%d],
              "clientTrigger": "BUTTON_FALLBACK"
            }
            """.formatted(noodleId);

        mockMvc.perform(post("/api/recommendations/shake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("暂无可摇出的餐厅"));
    }

    private void insertRestaurant(
        String name,
        int averagePrice,
        String cuisine,
        String tasteTags,
        String sceneTags,
        String avoidTags
    ) {
        jdbcTemplate.update("""
                INSERT INTO restaurant (
                    name, district, business_area, address, average_price, cuisine, rating,
                    taste_tags, scene_tags, avoid_tags, recommended_dishes, description,
                    latitude, longitude, is_demo_data, source_note, status
                )
                VALUES (?, '尖草坪区', '中北大学周边', ?, ?, ?, 4.5, ?, ?, ?, '[\"招牌\"]', 'SHAKE_TEST', 38.018, 112.447, 1, '自动化测试数据', 1)
                """,
            name,
            name + "地址",
            averagePrice,
            cuisine,
            tasteTags,
            sceneTags,
            avoidTags
        );
    }
}
