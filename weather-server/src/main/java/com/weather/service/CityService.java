package com.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.weather.entity.FollowedCity;
import com.weather.entity.User;
import com.weather.mapper.FollowedCityMapper;
import com.weather.mapper.UserMapper;
import com.weather.util.AdminUtils;
import com.weather.util.QWeatherApiClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 城市管理服务：关注/取消城市、城市列表
 */
@Service
public class CityService {

    private final FollowedCityMapper followedCityMapper;
    private final UserMapper userMapper;
    private final QWeatherApiClient qWeatherApiClient;
    private final AdminUtils adminUtils;

    public CityService(FollowedCityMapper followedCityMapper,
                       UserMapper userMapper,
                       QWeatherApiClient qWeatherApiClient,
                       AdminUtils adminUtils) {
        this.followedCityMapper = followedCityMapper;
        this.userMapper = userMapper;
        this.qWeatherApiClient = qWeatherApiClient;
        this.adminUtils = adminUtils;
    }

    /**
     * 用户关注城市
     *
     * 先去和风 API 搜索城市编码，再写入关注记录。
     * 重复关注会返回错误。
     */
    public Map<String, Object> addCity(Long userId, String city) {
        String trimmed = city.trim();
        if (trimmed.isEmpty() || trimmed.length() > 50) {
            throw new IllegalArgumentException("城市名长度不合法");
        }

        // 检查是否已关注过该城市
        LambdaQueryWrapper<FollowedCity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowedCity::getUserId, userId)
               .eq(FollowedCity::getCity, trimmed);
        if (followedCityMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("该城市已关注");
        }

        // 去和风天气 API 查询城市代码（同时也会写入本地 city_list 表）
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

    /** 获取当前用户关注的城市列表，按关注时间倒序 */
    public List<FollowedCity> listCities(Long userId) {
        LambdaQueryWrapper<FollowedCity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowedCity::getUserId, userId)
               .orderByDesc(FollowedCity::getCreatedAt);
        return followedCityMapper.selectList(wrapper);
    }

    /**
     * 管理员删除用户的关注城市
     *
     * 先校验操作者是否为管理员，再检查关注记录是否存在。
     */
    public void removeCity(Long userId, Integer cityId) {
        adminUtils.checkAdmin(userId);
        LambdaQueryWrapper<FollowedCity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowedCity::getId, cityId)
               .eq(FollowedCity::getUserId, userId);
        if (followedCityMapper.selectCount(wrapper) == 0) {
            throw new IllegalArgumentException("城市不存在或无权操作");
        }
        followedCityMapper.deleteById(cityId);
    }

    /** 获取所有被关注的城市名（去重，按字母排序） */
    public List<String> listAllCities() {
        QueryWrapper<FollowedCity> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT city").orderByAsc("city");
        return followedCityMapper.selectList(wrapper).stream()
                .map(FollowedCity::getCity)
                .collect(Collectors.toList());
    }

    /**
     * 管理员获取全部关注记录
     *
     * 从 followed_city 表查出所有记录，再按 userId 批量查询用户昵称进行填充。
     */
    public List<Map<String, Object>> listAllFollowedCities(Long adminUserId) {
        adminUtils.checkAdmin(adminUserId);

        List<FollowedCity> all = followedCityMapper.selectList(null);

        // 收集所有涉及的用户 ID，批量查询昵称（避免 N+1）
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

}
