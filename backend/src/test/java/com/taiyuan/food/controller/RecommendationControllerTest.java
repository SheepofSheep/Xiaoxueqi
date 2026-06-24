package com.taiyuan.food.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite:file:recommendation-controller-test?mode=memory&cache=shared",
    "spring.sql.init.mode=always",
    "app.deepseek.enabled=false"
})
@AutoConfigureMockMvc
class RecommendationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM recommendation_history WHERE request_json LIKE '%推荐测试%'");
        jdbcTemplate.update("DELETE FROM restaurant WHERE name LIKE '推荐测试%'");
        insertRestaurant(
            "推荐测试刀削面",
            "尖草坪区",
            "中北大学周边",
            22,
            "山西面食",
            4.6,
            "[\"咸香\",\"碳水\"]",
            "[\"一人食\",\"学生党\"]",
            "[\"太辣\"]",
            "[\"刀削面\",\"过油肉\"]"
        );
        insertRestaurant(
            "推荐测试烧烤",
            "尖草坪区",
            "中北大学周边",
            45,
            "烧烤",
            4.2,
            "[\"重辣\",\"油香\"]",
            "[\"朋友聚餐\",\"夜宵\"]",
            "[\"清淡\"]",
            "[\"烤串\",\"烤面筋\"]"
        );
        insertRestaurant(
            "推荐测试麻辣烫",
            "尖草坪区",
            "中北大学周边",
            25,
            "麻辣烫",
            4.3,
            "[\"微辣\",\"麻辣\"]",
            "[\"一人食\",\"快餐\"]",
            "[\"清淡\"]",
            "[\"麻辣烫\",\"宽粉\"]"
        );
        insertRestaurant(
            "推荐测试盖饭",
            "尖草坪区",
            "中北大学周边",
            30,
            "快餐",
            4.1,
            "[\"下饭\"]",
            "[\"学生党\",\"快餐\"]",
            "[\"清淡\"]",
            "[\"鸡腿饭\"]"
        );
    }

    @Test
    void recommendsRestaurantsByLocalScore() throws Exception {
        String body = """
            {
              "district": "尖草坪区",
              "businessArea": "中北大学周边",
              "maxPrice": 35,
              "cuisines": ["山西面食"],
              "tasteTags": ["碳水"],
              "sceneTags": ["一人食"],
              "limit": 3
            }
            """;

        mockMvc.perform(post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.items[0].restaurant.name").value("推荐测试刀削面"))
            .andExpect(jsonPath("$.data.items[0].matchScore", greaterThanOrEqualTo(80)))
            .andExpect(jsonPath("$.data.items[0].matchedTags[0]").exists())
            .andExpect(jsonPath("$.data.items[0].reason").exists())
            .andExpect(jsonPath("$.data.localReasonUsed").value(true))
            .andExpect(jsonPath("$.data.aiUsed").value(false))
            .andExpect(jsonPath("$.data.aiFallback").value(true));
    }

    @Test
    void keepsOverBudgetRestaurantsOutWhenEnoughBudgetMatches() throws Exception {
        String body = """
            {
              "district": "尖草坪区",
              "businessArea": "中北大学周边",
              "maxPrice": 35,
              "limit": 5
            }
            """;

        mockMvc.perform(post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.relaxed").value(false))
            .andExpect(jsonPath("$.data.items[*].restaurant.name", not(hasItem("推荐测试烧烤"))));
    }

    @Test
    void relaxesBudgetOnlyWhenStrictMatchesAreTooFew() throws Exception {
        String body = """
            {
              "district": "尖草坪区",
              "businessArea": "中北大学周边",
              "maxPrice": 35,
              "cuisines": ["烧烤"],
              "limit": 3
            }
            """;

        mockMvc.perform(post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.relaxed").value(true))
            .andExpect(jsonPath("$.data.relaxedRules", hasItem("预算放宽 10 元")))
            .andExpect(jsonPath("$.data.items[*].restaurant.name", hasItem("推荐测试烧烤")));
    }

    @Test
    void excludesAvoidTagsAndExcludedRestaurantIds() throws Exception {
        Long noodleId = jdbcTemplate.queryForObject(
            "SELECT id FROM restaurant WHERE name = '推荐测试刀削面'",
            Long.class
        );
        String body = """
            {
              "district": "尖草坪区",
              "businessArea": "中北大学周边",
              "maxPrice": 60,
              "avoidTags": ["清淡"],
              "excludeRestaurantIds": [%d],
              "limit": 5
            }
            """.formatted(noodleId);

        mockMvc.perform(post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void recordsRecommendationHistory() throws Exception {
        String body = """
            {
              "district": "尖草坪区",
              "businessArea": "中北大学周边",
              "maxPrice": 35,
              "cuisines": ["麻辣烫"],
              "limit": 2
            }
            """;

        mockMvc.perform(post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM recommendation_history WHERE trigger_type = 'NORMAL'",
            Integer.class
        );

        org.assertj.core.api.Assertions.assertThat(count).isNotNull().isGreaterThanOrEqualTo(1);
    }

    @Test
    void aiRecommendationFallsBackToLocalIntentParser() throws Exception {
        String body = """
            {
              "prompt": "中北附近想吃30元以内的微辣麻辣烫，一个人，学生党，别太油",
              "district": "尖草坪区",
              "businessArea": "中北大学周边",
              "maxPrice": 35,
              "limit": 3
            }
            """;

        mockMvc.perform(post("/api/recommendations/ai")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.intent.aiUsed").value(false))
            .andExpect(jsonPath("$.data.intent.aiFallback").value(true))
            .andExpect(jsonPath("$.data.intent.businessArea").value("中北大学周边"))
            .andExpect(jsonPath("$.data.intent.maxPrice").value(30))
            .andExpect(jsonPath("$.data.interpretedRequest.cuisines", hasItem("麻辣烫")))
            .andExpect(jsonPath("$.data.interpretedRequest.tasteTags", hasItem("微辣")))
            .andExpect(jsonPath("$.data.interpretedRequest.sceneTags", hasItem("一人食")))
            .andExpect(jsonPath("$.data.recommendation.items.length()", greaterThanOrEqualTo(1)));
    }

    private void insertRestaurant(
        String name,
        String district,
        String businessArea,
        int averagePrice,
        String cuisine,
        double rating,
        String tasteTags,
        String sceneTags,
        String avoidTags,
        String recommendedDishes
    ) {
        jdbcTemplate.update("""
                INSERT INTO restaurant (
                    name, district, business_area, address, average_price, cuisine, rating,
                    taste_tags, scene_tags, avoid_tags, recommended_dishes, description,
                    latitude, longitude, is_demo_data, source_note, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '推荐测试描述', 38.018, 112.447, 1, '自动化测试数据', 1)
                """,
            name,
            district,
            businessArea,
            name + "地址",
            averagePrice,
            cuisine,
            rating,
            tasteTags,
            sceneTags,
            avoidTags,
            recommendedDishes
        );
    }
}
