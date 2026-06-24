package com.taiyuan.food.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public final class TagConverter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TagConverter() {}

    public static List<String> jsonToTags(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.strip())) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    public static String tagsToJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public static List<String> csvToTags(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(csv.split("\\|"));
    }

    public static String tagsToCsv(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join("|", tags);
    }
}
