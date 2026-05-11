package com.weather.config;

import com.weather.entity.FollowedCity;
import com.weather.mapper.FollowedCityMapper;
import com.weather.service.WeatherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class WeatherScheduler {

    private final WeatherService weatherService;
    private final FollowedCityMapper followedCityMapper;

    public WeatherScheduler(WeatherService weatherService, FollowedCityMapper followedCityMapper) {
        this.weatherService = weatherService;
        this.followedCityMapper = followedCityMapper;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void fetchCurrentWeather() {
        Set<String> cities = weatherService.getAllFollowedCities();
        if (cities.isEmpty()) return;
        for (String city : cities) {
            try {
                weatherService.fetchNowForCity(city);
            } catch (Exception ignored) {
            }
        }
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void fetchForecastDaily() {
        Set<String> cities = weatherService.getAllFollowedCities();
        if (cities.isEmpty()) return;
        for (String city : cities) {
            try {
                weatherService.fetchForecastForCity(city);
            } catch (Exception ignored) {
            }
        }
    }
}
