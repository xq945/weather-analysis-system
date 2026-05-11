package com.weather.controller;

import com.weather.dto.Result;
import com.weather.entity.FollowedCity;
import com.weather.service.CityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @PostMapping
    public Result<FollowedCity> addCity(@RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String city = body.get("city");
        if (city == null || city.isBlank()) {
            return Result.error(400, "城市名不能为空");
        }
        FollowedCity result = cityService.addCity(userId, city);
        return Result.ok(result);
    }

    @GetMapping
    public Result<List<FollowedCity>> listCities(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<FollowedCity> cities = cityService.listCities(userId);
        return Result.ok(cities);
    }

    @DeleteMapping("/{id}")
    public Result<Void> removeCity(@PathVariable Integer id,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cityService.removeCity(userId, id);
        return Result.ok();
    }
}
