package com.taiyuan.food.controller;

import com.taiyuan.food.config.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite:file:csv-import-test?mode=memory&cache=shared",
    "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class AdminRestaurantImportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void cleanTestData() {
        jdbcTemplate.update("DELETE FROM restaurant WHERE name LIKE 'TDD%'");
    }

    @Test
    void importsCsvRowsIntoRestaurantTable() throws Exception {
        String csv = """
            name,district,business_area,address,average_price,cuisine,rating,taste_tags,scene_tags,avoid_tags,recommended_dishes,description,latitude,longitude,is_demo_data,source_note
            TDD刀削面,尖草坪区,中北大学周边,TDD测试地址1,22,山西面食,4.5,咸香|碳水,一人食|学生党,太辣,刀削面|过油肉,适合测试导入,38.018,112.447,1,自动化测试数据
            TDD麻辣烫,尖草坪区,中北大学周边,TDD测试地址2,25,麻辣烫,4.3,微辣|麻辣,一人食|快餐,清淡,麻辣烫|宽粉,适合测试导入,38.019,112.448,1,自动化测试数据
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "restaurants.csv",
            "text/csv",
            csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/restaurants/import")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.successCount").value(2))
            .andExpect(jsonPath("$.data.failureCount").value(0));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM restaurant WHERE name LIKE 'TDD%'",
            Integer.class
        );
        String tasteTags = jdbcTemplate.queryForObject(
            "SELECT taste_tags FROM restaurant WHERE name = 'TDD刀削面'",
            String.class
        );

        assertThat(count).isEqualTo(2);
        assertThat(tasteTags).isEqualTo("[\"咸香\",\"碳水\"]");
    }

    @Test
    void updatesRestaurantWhenNameAndAddressAlreadyExist() throws Exception {
        String firstCsv = """
            name,district,business_area,address,average_price,cuisine,rating,taste_tags,scene_tags,avoid_tags,recommended_dishes,description,latitude,longitude,is_demo_data,source_note
            TDD更新店,尖草坪区,中北大学周边,TDD固定地址,20,快餐,4.0,咸香,一人食,,盖饭,第一次导入,38.010,112.440,1,自动化测试数据
            """;
        String secondCsv = """
            name,district,business_area,address,average_price,cuisine,rating,taste_tags,scene_tags,avoid_tags,recommended_dishes,description,latitude,longitude,is_demo_data,source_note
            TDD更新店,尖草坪区,中北大学周边,TDD固定地址,28,快餐,4.6,咸香|下饭,一人食|学生党,,盖饭|鸡腿饭,第二次导入更新,38.011,112.441,1,自动化测试数据
            """;

        mockMvc.perform(multipart("/api/admin/restaurants/import")
                .file(new MockMultipartFile("file", "first.csv", "text/csv", firstCsv.getBytes(StandardCharsets.UTF_8)))
                .header(HttpHeaders.AUTHORIZATION, adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.createdCount").value(1));

        mockMvc.perform(multipart("/api/admin/restaurants/import")
                .file(new MockMultipartFile("file", "second.csv", "text/csv", secondCsv.getBytes(StandardCharsets.UTF_8)))
                .header(HttpHeaders.AUTHORIZATION, adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.updatedCount").value(1));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM restaurant WHERE name = 'TDD更新店' AND address = 'TDD固定地址'",
            Integer.class
        );
        Double price = jdbcTemplate.queryForObject(
            "SELECT average_price FROM restaurant WHERE name = 'TDD更新店'",
            Double.class
        );
        String dishes = jdbcTemplate.queryForObject(
            "SELECT recommended_dishes FROM restaurant WHERE name = 'TDD更新店'",
            String.class
        );

        assertThat(count).isEqualTo(1);
        assertThat(price).isEqualTo(28);
        assertThat(dishes).isEqualTo("[\"盖饭\",\"鸡腿饭\"]");
    }

    private String adminBearerToken() {
        return "Bearer " + jwtUtil.generateToken("admin", "ADMIN");
    }
}
