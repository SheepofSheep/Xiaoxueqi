package com.taiyuan.food.service;

import com.taiyuan.food.common.TagConverter;
import com.taiyuan.food.vo.CsvImportErrorVO;
import com.taiyuan.food.vo.RestaurantImportResultVO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class RestaurantCsvImportService {
    private final JdbcTemplate jdbcTemplate;

    public RestaurantCsvImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public RestaurantImportResultVO importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new RestaurantImportResultVO(0, 0, 0, 1, List.of(new CsvImportErrorVO(0, "CSV 文件不能为空")));
        }

        int createdCount = 0;
        int updatedCount = 0;
        List<CsvImportErrorVO> errors = new ArrayList<>();

        try (CSVParser parser = parse(file)) {
            for (CSVRecord record : parser) {
                long rowNumber = record.getRecordNumber() + 1;
                try {
                    RestaurantCsvRow row = toRow(record);
                    if (exists(row.name(), row.address())) {
                        update(row);
                        updatedCount++;
                    } else {
                        insert(row);
                        createdCount++;
                    }
                } catch (IllegalArgumentException ex) {
                    errors.add(new CsvImportErrorVO(rowNumber, ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            return new RestaurantImportResultVO(0, 0, 0, 1, List.of(new CsvImportErrorVO(0, "CSV 读取失败：" + ex.getMessage())));
        }

        int successCount = createdCount + updatedCount;
        return new RestaurantImportResultVO(successCount, createdCount, updatedCount, errors.size(), errors);
    }

    private CSVParser parse(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();
        return format.parse(new StringReader(content));
    }

    private RestaurantCsvRow toRow(CSVRecord record) {
        String name = required(record, "name");
        String district = required(record, "district");
        BigDecimal averagePrice = decimal(required(record, "average_price"), "average_price");
        String cuisine = required(record, "cuisine");
        BigDecimal rating = decimal(optional(record, "rating", "0"), "rating");
        if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("rating 必须在 0-5 之间");
        }

        return new RestaurantCsvRow(
            name,
            district,
            blankToNull(optional(record, "business_area", "")),
            blankToNull(optional(record, "address", "")),
            averagePrice,
            cuisine,
            rating,
            tags(optional(record, "taste_tags", "")),
            tags(optional(record, "scene_tags", "")),
            tags(optional(record, "avoid_tags", "")),
            tags(optional(record, "recommended_dishes", "")),
            blankToNull(optional(record, "description", "")),
            optionalDecimal(record, "latitude"),
            optionalDecimal(record, "longitude"),
            demoFlag(optional(record, "is_demo_data", "0")),
            blankToNull(optional(record, "source_note", ""))
        );
    }

    private String required(CSVRecord record, String field) {
        String value = optional(record, field, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.trim();
    }

    private String optional(CSVRecord record, String field, String defaultValue) {
        if (!record.isMapped(field) || !record.isSet(field)) {
            return defaultValue;
        }
        String value = record.get(field);
        return value == null ? defaultValue : value.trim();
    }

    private BigDecimal decimal(String value, String field) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " 必须是数字");
        }
    }

    private BigDecimal optionalDecimal(CSVRecord record, String field) {
        String value = optional(record, field, "");
        return value.isBlank() ? null : decimal(value, field);
    }

    private int demoFlag(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        if (Objects.equals(value, "1") || Objects.equals(value, "0")) {
            return Integer.parseInt(value);
        }
        throw new IllegalArgumentException("is_demo_data 只能是 1 或 0");
    }

    private String tags(String raw) {
        return TagConverter.tagsToJson(TagConverter.csvToTags(raw));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean exists(String name, String address) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM restaurant WHERE name = ? AND COALESCE(address, '') = COALESCE(?, '')",
            Integer.class,
            name,
            address
        );
        return count != null && count > 0;
    }

    private void insert(RestaurantCsvRow row) {
        jdbcTemplate.update("""
                INSERT INTO restaurant (
                    name, district, business_area, address, average_price, cuisine, rating,
                    taste_tags, scene_tags, avoid_tags, recommended_dishes, description,
                    latitude, longitude, is_demo_data, source_note, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """,
            row.name(), row.district(), row.businessArea(), row.address(), row.averagePrice(), row.cuisine(), row.rating(),
            row.tasteTags(), row.sceneTags(), row.avoidTags(), row.recommendedDishes(), row.description(),
            row.latitude(), row.longitude(), row.isDemoData(), row.sourceNote()
        );
    }

    private void update(RestaurantCsvRow row) {
        jdbcTemplate.update("""
                UPDATE restaurant
                SET district = ?,
                    business_area = ?,
                    average_price = ?,
                    cuisine = ?,
                    rating = ?,
                    taste_tags = ?,
                    scene_tags = ?,
                    avoid_tags = ?,
                    recommended_dishes = ?,
                    description = ?,
                    latitude = ?,
                    longitude = ?,
                    is_demo_data = ?,
                    source_note = ?,
                    status = 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE name = ? AND COALESCE(address, '') = COALESCE(?, '')
                """,
            row.district(), row.businessArea(), row.averagePrice(), row.cuisine(), row.rating(),
            row.tasteTags(), row.sceneTags(), row.avoidTags(), row.recommendedDishes(), row.description(),
            row.latitude(), row.longitude(), row.isDemoData(), row.sourceNote(), row.name(), row.address()
        );
    }

    private record RestaurantCsvRow(
        String name,
        String district,
        String businessArea,
        String address,
        BigDecimal averagePrice,
        String cuisine,
        BigDecimal rating,
        String tasteTags,
        String sceneTags,
        String avoidTags,
        String recommendedDishes,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        int isDemoData,
        String sourceNote
    ) {
    }
}
