package com.weather.controller;

import com.weather.dto.Result;
import com.weather.entity.FollowedCity;
import com.weather.service.CityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 城市管理接口：关注/取消城市、城市列表
 */
@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    /**
     * 关注城市
     *
     * 先去和风 API 搜索城市代码，写入数据库。重复关注会返回错误。
     *
     * @param body    请求体，包含 city 字段
     * @param request HTTP 请求
     * @return 关注记录（id, city, cityCode, createdAt）
     */
    @PostMapping
    public Result<?> addCity(@RequestBody Map<String, String> body,
                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String city = body.get("city");
        if (city == null || city.isBlank()) {
            return Result.error(400, "城市名不能为空");
        }
        try {
            Map<String, Object> result = cityService.addCity(userId, city);
            return Result.ok(result);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 获取当前用户的关注城市列表
     */
    @GetMapping
    public Result<List<FollowedCity>> listCities(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<FollowedCity> cities = cityService.listCities(userId);
        return Result.ok(cities);
    }

    /**
     * 获取所有被关注的城市名（去重后）
     */
    @GetMapping("/all")
    public Result<List<String>> listAllCities() {
        return Result.ok(cityService.listAllCities());
    }

    /**
     * 管理员获取所有用户的关注城市记录（含关注者昵称）
     */
    @GetMapping("/admin/all")
    public Result<?> listAllFollowedCities(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            return Result.ok(cityService.listAllFollowedCities(userId));
        } catch (RuntimeException e) {
            return Result.error(403, e.getMessage());
        }
    }

    /**
     * 取消关注城市
     *
     * @param id 关注记录的 ID（路径参数）
     */
    @DeleteMapping("/{id}")
    public Result<Void> removeCity(@PathVariable Integer id,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cityService.removeCity(userId, id);
        return Result.ok();
    }
}
