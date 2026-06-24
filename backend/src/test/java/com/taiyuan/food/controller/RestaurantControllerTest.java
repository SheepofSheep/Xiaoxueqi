package com.taiyuan.food.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite:file:restaurant-controller-test?mode=memory&cache=shared",
    "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class RestaurantControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM restaurant WHERE name LIKE '查询测试%'");
        insertRestaurant(
            "查询测试刀削面",
            "尖草坪区",
            "中北大学周边",
            "查询测试地址1",
            22,
            "山西面食",
            4.5,
            "[\"咸香\",\"碳水\"]",
            "[\"一人食\",\"学生党\"]",
            "[\"太辣\"]",
            "[\"刀削面\",\"过油肉\"]",
            "适合查询测试",
            1
        );
        insertRestaurant(
            "查询测试麻辣烫",
            "尖草坪区",
            "中北大学周边",
            "查询测试地址2",
            25,
            "麻辣烫",
            4.3,
            "[\"微辣\",\"麻辣\"]",
            "[\"一人食\",\"快餐\"]",
            "[\"清淡\"]",
            "[\"麻辣烫\",\"宽粉\"]",
            "适合查询测试",
            1
        );
        insertRestaurant(
            "查询测试下架店",
            "尖草坪区",
            "中北大学周边",
            "查询测试地址3",
            20,
            "快餐",
            4.0,
            "[\"咸香\"]",
            "[\"快餐\"]",
            "[]",
            "[\"盖饭\"]",
            "不应出现在用户列表",
            0
        );
    }

    @Test
    void listsActiveRestaurantsWithPaginationAndFilters() throws Exception {
        mockMvc.perform(get("/api/restaurants")
                .param("district", "尖草坪区")
                .param("businessArea", "中北大学周边")
                .param("tasteTag", "微辣")
                .param("page", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(10))
            .andExpect(jsonPath("$.data.list[0].name").value("查询测试麻辣烫"))
            .andExpect(jsonPath("$.data.list[0].tasteTags[0]").value("微辣"))
            .andExpect(jsonPath("$.data.list[0].sceneTags[1]").value("快餐"))
            .andExpect(jsonPath("$.data.list[0].isDemoData").value(true));
    }

    @Test
    void getsRestaurantDetailById() throws Exception {
        Long id = jdbcTemplate.queryForObject(
            "SELECT id FROM restaurant WHERE name = '查询测试刀削面'",
            Long.class
        );

        mockMvc.perform(get("/api/restaurants/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(id))
            .andExpect(jsonPath("$.data.name").value("查询测试刀削面"))
            .andExpect(jsonPath("$.data.recommendedDishes[1]").value("过油肉"))
            .andExpect(jsonPath("$.data.avoidTags[0]").value("太辣"));
    }

    @Test
    void returns404WhenRestaurantMissingOrInactive() throws Exception {
        Long inactiveId = jdbcTemplate.queryForObject(
            "SELECT id FROM restaurant WHERE name = '查询测试下架店'",
            Long.class
        );

        mockMvc.perform(get("/api/restaurants/{id}", inactiveId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("餐厅不存在或已下架"));
    }

    private void insertRestaurant(
        String name,
        String district,
        String businessArea,
        String address,
        int averagePrice,
        String cuisine,
        double rating,
        String tasteTags,
        String sceneTags,
        String avoidTags,
        String recommendedDishes,
        String description,
        int status
    ) {
        jdbcTemplate.update("""
                INSERT INTO restaurant (
                    name, district, business_area, address, average_price, cuisine, rating,
                    taste_tags, scene_tags, avoid_tags, recommended_dishes, description,
                    latitude, longitude, is_demo_data, source_note, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 38.018, 112.447, 1, '自动化测试数据', ?)
                """,
            name,
            district,
            businessArea,
            address,
            averagePrice,
            cuisine,
            rating,
            tasteTags,
            sceneTags,
            avoidTags,
            recommendedDishes,
            description,
            status
        );
    }
}
