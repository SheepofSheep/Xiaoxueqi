package com.taiyuan.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taiyuan.food.entity.RestaurantEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RestaurantMapper extends BaseMapper<RestaurantEntity> {
}
