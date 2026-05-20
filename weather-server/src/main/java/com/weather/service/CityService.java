package com.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.weather.entity.FollowedCity;
import com.weather.entity.User;
import com.weather.mapper.FollowedCityMapper;
import com.weather.mapper.UserMapper;
import com.weather.util.QWeatherApiClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CityService {

    private final FollowedCityMapper followedCityMapper;
    private final UserMapper userMapper;
    private final QWeatherApiClient qWeatherApiClient;

    public CityService(FollowedCityMapper followedCityMapper,
                       UserMapper userMapper,
                       QWeatherApiClient qWeatherApiClient) {
        this.followedCityMapper = followedCityMapper;
        this.userMapper = userMapper;
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
        checkAdmin(userId);
        LambdaQueryWrapper<FollowedCity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowedCity::getId, cityId)
               .eq(FollowedCity::getUserId, userId);
        if (followedCityMapper.selectCount(wrapper) == 0) {
            throw new IllegalArgumentException("城市不存在或无权操作");
        }
        followedCityMapper.deleteById(cityId);
    }

    public List<String> listAllCities() {
        QueryWrapper<FollowedCity> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT city").orderByAsc("city");
        return followedCityMapper.selectList(wrapper).stream()
                .map(FollowedCity::getCity)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> listAllFollowedCities(Long adminUserId) {
        checkAdmin(adminUserId);

        List<FollowedCity> all = followedCityMapper.selectList(null);

        List<Integer> userIds = all.stream()
                .map(FollowedCity::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, String> nicknameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : "未知用户"));

        return all.stream().map(fc -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", fc.getId());
            item.put("city", fc.getCity());
            item.put("userId", fc.getUserId());
            item.put("createdAt", fc.getCreatedAt());
            item.put("nickname", nicknameMap.getOrDefault(fc.getUserId(), "未知用户"));
            return item;
        }).collect(Collectors.toList());
    }

    private void checkAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getPermission() == null || user.getPermission() != 2) {
            throw new IllegalArgumentException("无权限访问");
        }
    }
}
