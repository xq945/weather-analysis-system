package com.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weather.entity.WeatherReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分析报告 Mapper
 */
@Mapper
public interface WeatherReportMapper extends BaseMapper<WeatherReport> {
}
