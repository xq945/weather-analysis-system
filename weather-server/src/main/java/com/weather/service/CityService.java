package com.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.FollowedCity;
import com.weather.mapper.FollowedCityMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CityService {

    private final FollowedCityMapper followedCityMapper;

    public CityService(FollowedCityMapper followedCityMapper) {
        this.followedCityMapper = followedCityMapper;
    }

    public FollowedCity addCity(Long userId, String city) {
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

        FollowedCity entity = new FollowedCity();
        entity.setUserId(userId.intValue());
        entity.setCity(trimmed);
        followedCityMapper.insert(entity);
        return entity;
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
