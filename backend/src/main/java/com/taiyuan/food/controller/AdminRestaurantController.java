package com.taiyuan.food.controller;

import com.taiyuan.food.common.ApiResponse;
import com.taiyuan.food.common.TagConverter;
import com.taiyuan.food.dto.RestaurantCreateDTO;
import com.taiyuan.food.service.AmapFetchService;
import com.taiyuan.food.entity.RestaurantEntity;
import com.taiyuan.food.mapper.RestaurantMapper;
import com.taiyuan.food.service.RestaurantCsvImportService;
import com.taiyuan.food.vo.RestaurantImportResultVO;
import com.taiyuan.food.vo.RestaurantVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/restaurants")
public class AdminRestaurantController {
    private final RestaurantCsvImportService csvImportService;
    private final RestaurantMapper restaurantMapper;
    private final AmapFetchService amapFetchService;

    public AdminRestaurantController(RestaurantCsvImportService csvImportService,
                                     RestaurantMapper restaurantMapper,
                                     AmapFetchService amapFetchService) {
        this.csvImportService = csvImportService;
        this.restaurantMapper = restaurantMapper;
        this.amapFetchService = amapFetchService;
    }

    @PostMapping
    public ApiResponse<RestaurantVO> create(@Valid @RequestBody RestaurantCreateDTO dto) {
        RestaurantEntity entity = toEntity(dto);
        entity.setStatus(1);
        restaurantMapper.insert(entity);
        return ApiResponse.success(RestaurantVO.from(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<RestaurantVO> update(@PathVariable long id, @Valid @RequestBody RestaurantCreateDTO dto) {
        RestaurantEntity entity = restaurantMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.fail(404, "餐厅不存在");
        }
        applyDto(entity, dto);
        restaurantMapper.updateById(entity);
        return ApiResponse.success(RestaurantVO.from(restaurantMapper.selectById(id)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable long id) {
        RestaurantEntity entity = restaurantMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.fail(404, "餐厅不存在");
        }
        entity.setStatus(0);
        restaurantMapper.updateById(entity);
        return ApiResponse.success(null);
    }

    @PostMapping("/import")
    public ApiResponse<RestaurantImportResultVO> importRestaurants(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(csvImportService.importCsv(file));
    }

    @PostMapping("/amap-fetch")
    public ApiResponse<?> fetchFromAmap(
        @RequestParam double lng,
        @RequestParam double lat,
        @RequestParam(defaultValue = "5000") int radius,
        @RequestParam(defaultValue = "中北大学周边") String businessArea,
        @RequestParam(defaultValue = "尖草坪区") String district,
        @RequestParam(defaultValue = "3") int pages
    ) {
        int saved = amapFetchService.fetchAndSave(lng, lat, radius, businessArea, district, pages);
        return ApiResponse.success(java.util.Map.of("saved", saved, "area", businessArea));
    }

    private RestaurantEntity toEntity(RestaurantCreateDTO dto) {
        RestaurantEntity e = new RestaurantEntity();
        applyDto(e, dto);
        return e;
    }

    private void applyDto(RestaurantEntity e, RestaurantCreateDTO dto) {
        e.setName(dto.getName());
        e.setDistrict(dto.getDistrict());
        e.setBusinessArea(dto.getBusinessArea());
        e.setAddress(dto.getAddress());
        e.setAveragePrice(dto.getAveragePrice());
        e.setCuisine(dto.getCuisine());
        e.setRating(dto.getRating());
        e.setTasteTags(TagConverter.tagsToJson(dto.getTasteTags()));
        e.setSceneTags(TagConverter.tagsToJson(dto.getSceneTags()));
        e.setAvoidTags(TagConverter.tagsToJson(dto.getAvoidTags()));
        e.setRecommendedDishes(TagConverter.tagsToJson(dto.getRecommendedDishes()));
        e.setDescription(dto.getDescription());
        e.setLatitude(dto.getLatitude());
        e.setLongitude(dto.getLongitude());
        e.setCoverImage(dto.getCoverImage());
        e.setSourceNote(dto.getSourceNote());
        e.setIsDemoData(dto.getIsDemoData() != null ? dto.getIsDemoData() : 1);
    }
}
