package com.taiyuan.food.service;

import com.taiyuan.food.common.TagConverter;
import com.taiyuan.food.vo.PageResultVO;
import com.taiyuan.food.vo.RestaurantVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RestaurantQueryService {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<RestaurantVO> rowMapper = this::mapRestaurant;

    public RestaurantQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResultVO<RestaurantVO> list(
        String keyword,
        String district,
        String businessArea,
        String cuisine,
        BigDecimal maxPrice,
        String tasteTag,
        String sceneTag,
        Integer page,
        Integer pageSize
    ) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        StringBuilder where = new StringBuilder(" WHERE status = 1");
        List<Object> params = new ArrayList<>();
        appendTextFilter(where, params, "district", district);
        appendTextFilter(where, params, "business_area", businessArea);
        appendTextFilter(where, params, "cuisine", cuisine);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (name LIKE ? OR address LIKE ? OR description LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (maxPrice != null) {
            where.append(" AND average_price <= ?");
            params.add(maxPrice);
        }
        appendTagFilter(where, params, "taste_tags", tasteTag);
        appendTagFilter(where, params, "scene_tags", sceneTag);

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM restaurant" + where,
            Long.class,
            params.toArray()
        );

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(safePageSize);
        pageParams.add((safePage - 1) * safePageSize);
        List<RestaurantVO> list = jdbcTemplate.query(
            """
                SELECT *
                FROM restaurant
                """ + where + """

                ORDER BY rating DESC, id DESC
                LIMIT ? OFFSET ?
                """,
            rowMapper,
            pageParams.toArray()
        );

        return new PageResultVO<>(list, safePage, safePageSize, total == null ? 0 : total);
    }

    public RestaurantVO findActiveById(long id) {
        List<RestaurantVO> list = jdbcTemplate.query(
            "SELECT * FROM restaurant WHERE id = ? AND status = 1",
            rowMapper,
            id
        );
        return list.isEmpty() ? null : list.getFirst();
    }

    private void appendTextFilter(StringBuilder where, List<Object> params, String column, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(column).append(" = ?");
            params.add(value.trim());
        }
    }

    private void appendTagFilter(StringBuilder where, List<Object> params, String column, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(column).append(" LIKE ?");
            params.add("%\"" + value.trim() + "\"%");
        }
    }

    private RestaurantVO mapRestaurant(ResultSet rs, int rowNum) throws SQLException {
        return new RestaurantVO(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("district"),
            rs.getString("business_area"),
            rs.getString("address"),
            rs.getBigDecimal("average_price"),
            rs.getString("cuisine"),
            rs.getBigDecimal("rating"),
            TagConverter.jsonToTags(rs.getString("taste_tags")),
            TagConverter.jsonToTags(rs.getString("scene_tags")),
            TagConverter.jsonToTags(rs.getString("avoid_tags")),
            TagConverter.jsonToTags(rs.getString("recommended_dishes")),
            rs.getString("description"),
            rs.getBigDecimal("latitude"),
            rs.getBigDecimal("longitude"),
            rs.getString("cover_image"),
            rs.getString("source"),
            rs.getString("source_note"),
            rs.getInt("is_demo_data") == 1,
            rs.getInt("status"),
            readDateTime(rs.getString("created_at")),
            readDateTime(rs.getString("updated_at"))
        );
    }

    private LocalDateTime readDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.replace(' ', 'T'));
    }
}
