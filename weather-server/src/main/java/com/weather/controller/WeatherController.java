package com.weather.controller;

import com.weather.dto.Result;
import com.weather.entity.WeatherData;
import com.weather.entity.WeatherForecast;
import com.weather.service.WeatherService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 天气数据接口：实时天气、预报、历史、统计、城市对比
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * 获取关注城市的天气概览（每城市最新一条实时数据）
     */
    @GetMapping("/overview")
    public Result<?> getOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(weatherService.getOverview(userId));
    }

    /**
     * 手动拉取所有关注城市的实时 + 预报数据
     */
    @PostMapping("/fetch")
    public Result<Map<String, Object>> fetchWeather(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = weatherService.fetchAllForUser(userId);
        return Result.ok(result);
    }

    /**
     * 手动拉取指定城市的实时 + 预报数据
     */
    @PostMapping("/fetch-city")
    public Result<?> fetchCityWeather(@RequestParam String city) {
        Map<String, Object> result = weatherService.fetchForCity(city);
        return Result.ok(result);
    }

    /** 获取指定城市最新实时天气 */
    @GetMapping("/now")
    public Result<WeatherData> getNow(@RequestParam String city) {
        WeatherData data = weatherService.getLatestNow(city);
        return Result.ok(data);
    }

    /** 获取指定城市 7 天预报 */
    @GetMapping("/forecast")
    public Result<List<WeatherForecast>> getForecast(@RequestParam String city) {
        List<WeatherForecast> list = weatherService.getForecast(city);
        return Result.ok(list);
    }

    /** 获取指定城市历史天气数据，默认近 7 天 */
    @GetMapping("/history")
    public Result<List<WeatherData>> getHistory(@RequestParam String city,
                                                 @RequestParam(defaultValue = "7") int days) {
        List<WeatherData> list = weatherService.getHistory(city, days);
        return Result.ok(list);
    }

    /** 获取指定城市的统计分析（均值、极值、趋势、逐日汇总） */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(@RequestParam String city,
                                                      @RequestParam(defaultValue = "7") int days) {
        return Result.ok(weatherService.getStatistics(city, days));
    }

    /** 两城市天气对比分析（温度/湿度/风力差值） */
    @GetMapping("/compare")
    public Result<Map<String, Object>> getCompare(@RequestParam String cityA,
                                                   @RequestParam String cityB,
                                                   @RequestParam(defaultValue = "7") int days) {
        return Result.ok(weatherService.getCompare(cityA, cityB, days));
    }
}
