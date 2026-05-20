package com.weather.config;

import com.weather.entity.FollowedCity;
import com.weather.mapper.FollowedCityMapper;
import com.weather.service.ReportService;
import com.weather.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class WeatherScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeatherScheduler.class);

    private final WeatherService weatherService;
    private final FollowedCityMapper followedCityMapper;
    private final ReportService reportService;

    public WeatherScheduler(WeatherService weatherService,
                            FollowedCityMapper followedCityMapper,
                            ReportService reportService) {
        this.weatherService = weatherService;
        this.followedCityMapper = followedCityMapper;
        this.reportService = reportService;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void fetchCurrentWeather() {
        Set<String> cities = weatherService.getAllFollowedCities();
        if (cities.isEmpty()) return;
        for (String city : cities) {
            try {
                weatherService.fetchNowForCity(city);
            } catch (Exception e) {
                log.error("定时获取实时天气失败: city={}", city, e);
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
            } catch (Exception e) {
                log.error("定时获取预报数据失败: city={}", city, e);
            }
        }
    }

    // 每日 09:00 生成昨日天气日报
    @Scheduled(cron = "0 0 9 * * *")
    public void generateDailyReports() {
        log.info("开始生成每日天气报告...");
        Set<String> cities = weatherService.getAllFollowedCities();
        if (cities.isEmpty()) {
            log.info("无关注城市，跳过报告生成");
            return;
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int success = 0;
        int fail = 0;
        for (String city : cities) {
            try {
                reportService.generateReport(city, yesterday, 1);
                success++;
            } catch (Exception e) {
                log.error("生成报告失败: city={}, error={}", city, e.getMessage());
                fail++;
            }
        }
        log.info("每日报告生成完成: 成功={}, 失败={}, 城市数={}", success, fail, cities.size());
    }

    // 每 30 分钟对账未同步向量
    @Scheduled(cron = "0 */30 * * * *")
    public void reconcileQdrantSync() {
        int unsynced = reportService.countUnsyncedReports();
        if (unsynced > 10) {
            log.warn("未同步向量报告数超标: {}", unsynced);
        }
    }
}
