package com.weather.controller;

import com.weather.dto.Result;
import com.weather.entity.WeatherData;
import com.weather.entity.WeatherForecast;
import com.weather.service.WeatherService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/overview")
    public Result<?> getOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(weatherService.getOverview(userId));
    }

    @PostMapping("/fetch")
    public Result<Map<String, Object>> fetchWeather(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = weatherService.fetchAllForUser(userId);
        return Result.ok(result);
    }

    @PostMapping("/fetch-city")
    public Result<?> fetchCityWeather(@RequestParam String city) {
        Map<String, Object> result = weatherService.fetchForCity(city);
        return Result.ok(result);
    }

    @GetMapping("/now")
    public Result<WeatherData> getNow(@RequestParam String city) {
        WeatherData data = weatherService.getLatestNow(city);
        return Result.ok(data);
    }

    @GetMapping("/forecast")
    public Result<List<WeatherForecast>> getForecast(@RequestParam String city) {
        List<WeatherForecast> list = weatherService.getForecast(city);
        return Result.ok(list);
    }

    @GetMapping("/history")
    public Result<List<WeatherData>> getHistory(@RequestParam String city,
                                                 @RequestParam(defaultValue = "7") int days) {
        List<WeatherData> list = weatherService.getHistory(city, days);
        return Result.ok(list);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(@RequestParam String city,
                                                      @RequestParam(defaultValue = "7") int days) {
        return Result.ok(weatherService.getStatistics(city, days));
    }

    @GetMapping("/compare")
    public Result<Map<String, Object>> getCompare(@RequestParam String cityA,
                                                   @RequestParam String cityB,
                                                   @RequestParam(defaultValue = "7") int days) {
        return Result.ok(weatherService.getCompare(cityA, cityB, days));
    }
}
