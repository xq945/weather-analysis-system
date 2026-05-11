package com.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.FollowedCity;
import com.weather.mapper.FollowedCityMapper;
import com.weather.util.QWeatherApiClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CityService {

    private final FollowedCityMapper followedCityMapper;
    private final QWeatherApiClient qWeatherApiClient;

    public CityService(FollowedCityMapper followedCityMapper,
                       QWeatherApiClient qWeatherApiClient) {
        this.followedCityMapper = followedCityMapper;
        this.qWeatherApiClient = qWeatherApiClient;
    }

    public Map<String, Object> addCity(Long userId, String city) {
        String trimmed = city.trim();
        if (trimmed.isEmpty() || trimmed.length() > 50) {
            throw new IllegalArgumentException("城市名长度不合法");
        }

        LambdaQueryWrapper<FollowedCity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowedCity::getUserId, userId)
               .eq(FollowedCity::getCity, trimmed);
        if (followedCityMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("该城市已关注");
        }

        String cityCode = qWeatherApiClient.searchCity(trimmed);

        FollowedCity entity = new FollowedCity();
        entity.setUserId(userId.intValue());
        entity.setCity(trimmed);
        followedCityMapper.insert(entity);

        Map<String, Object> result = new HashMap<>();
        result.put("id", entity.getId());
        result.put("city", entity.getCity());
        result.put("cityCode", cityCode);
        result.put("createdAt", entity.getCreatedAt());
        return result;
    }

    public List<FollowedCity> listCities(Long userId) {
        LambdaQueryWrapper<FollowedCity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowedCity::getUserId, userId)
               .orderByDesc(FollowedCity::getCreatedAt);
        return followedCityMapper.selectList(wrapper);
    }

    public void removeCity(Long userId, Integer cityId) {
        LambdaQueryWrapper<FollowedCity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowedCity::getId, cityId)
               .eq(FollowedCity::getUserId, userId);
        if (followedCityMapper.selectCount(wrapper) == 0) {
            throw new IllegalArgumentException("城市不存在或无权操作");
        }
        followedCityMapper.deleteById(cityId);
    }
}
