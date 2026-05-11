package com.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weather.entity.WeatherForecast;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WeatherForecastMapper extends BaseMapper<WeatherForecast> {
}
