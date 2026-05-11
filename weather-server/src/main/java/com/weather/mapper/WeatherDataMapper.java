package com.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weather.entity.WeatherData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WeatherDataMapper extends BaseMapper<WeatherData> {
}
