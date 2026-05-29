package com.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weather.entity.CityList;
import org.apache.ibatis.annotations.Mapper;

/**
 * 城市列表 Mapper
 */
@Mapper
public interface CityListMapper extends BaseMapper<CityList> {
}
