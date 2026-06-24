package com.taiyuan.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("restaurant")
public class RestaurantEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String district;
    private String businessArea;
    private String address;
    private BigDecimal averagePrice;
    private String cuisine;
    private BigDecimal rating;
    private String tasteTags;
    private String sceneTags;
    private String avoidTags;
    private String recommendedDishes;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String coverImage;
    private String source;
    private String sourceNote;
    private Integer isDemoData;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getBusinessArea() {
        return businessArea;
    }

    public void setBusinessArea(String businessArea) {
        this.businessArea = businessArea;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getTasteTags() {
        return tasteTags;
    }

    public void setTasteTags(String tasteTags) {
        this.tasteTags = tasteTags;
    }

    public String getSceneTags() {
        return sceneTags;
    }

    public void setSceneTags(String sceneTags) {
        this.sceneTags = sceneTags;
    }

    public String getAvoidTags() {
        return avoidTags;
    }

    public void setAvoidTags(String avoidTags) {
        this.avoidTags = avoidTags;
    }

    public String getRecommendedDishes() {
        return recommendedDishes;
    }

    public void setRecommendedDishes(String recommendedDishes) {
        this.recommendedDishes = recommendedDishes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceNote() {
        return sourceNote;
    }

    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }

    public Integer getIsDemoData() {
        return isDemoData;
    }

    public void setIsDemoData(Integer isDemoData) {
        this.isDemoData = isDemoData;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
