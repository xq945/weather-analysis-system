package com.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weather.entity.FollowedCity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注城市 Mapper
 */
@Mapper
public interface FollowedCityMapper extends BaseMapper<FollowedCity> {
}
