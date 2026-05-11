package com.weather.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weather.entity.CityList;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CityListMapper extends BaseMapper<CityList> {
}
