package com.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.weather.entity.FollowedCity;
import com.weather.entity.WeatherData;
import com.weather.entity.WeatherForecast;
import com.weather.mapper.FollowedCityMapper;
import com.weather.mapper.WeatherDataMapper;
import com.weather.mapper.WeatherForecastMapper;
import com.weather.util.QWeatherApiClient;
import com.weather.util.WeatherTextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final QWeatherApiClient apiClient;
    private final WeatherDataMapper weatherDataMapper;
    private final WeatherForecastMapper forecastMapper;
    private final FollowedCityMapper followedCityMapper;
    private final EmbeddingService embeddingService;
    private final RetrieverService retrieverService;

    public WeatherService(QWeatherApiClient apiClient,
                          WeatherDataMapper weatherDataMapper,
                          WeatherForecastMapper forecastMapper,
                          FollowedCityMapper followedCityMapper,
                          EmbeddingService embeddingService,
                          RetrieverService retrieverService) {
        this.apiClient = apiClient;
        this.weatherDataMapper = weatherDataMapper;
        this.forecastMapper = forecastMapper;
        this.followedCityMapper = followedCityMapper;
        this.embeddingService = embeddingService;
        this.retrieverService = retrieverService;
    }

    public WeatherData fetchNowForCity(String city) {
        String cityId = apiClient.searchCity(city);
        JsonNode now = apiClient.getWeatherNow(cityId);

        WeatherData data = new WeatherData();
        data.setCity(city);
        data.setObsTime(LocalDateTime.parse(now.path("obsTime").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        data.setTemp((float) now.path("temp").asDouble());
        data.setFeelsLike((float) now.path("feelsLike").asDouble());
        data.setHumidity((float) now.path("humidity").asDouble());
        data.setWindSpeed((float) now.path("windSpeed").asDouble());
        data.setPressure((float) now.path("pressure").asDouble());
        data.setVisibility((float) now.path("vis").asDouble());
        data.setWeatherText(WeatherTextUtil.toChinese(now.path("text").asText()));
        weatherDataMapper.insert(data);
        return data;
    }

    public List<WeatherForecast> fetchForecastForCity(String city) {
        String cityId = apiClient.searchCity(city);
        JsonNode daily = apiClient.getWeather7d(cityId);

        List<WeatherForecast> result = new ArrayList<>();
        for (JsonNode day : daily) {
            LocalDate forecastDate = LocalDate.parse(day.path("fxDate").asText());

            LambdaQueryWrapper<WeatherForecast> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WeatherForecast::getCity, city)
                   .eq(WeatherForecast::getForecastDate, forecastDate);
            if (forecastMapper.selectCount(wrapper) > 0) {
                continue;
            }

            WeatherForecast forecast = new WeatherForecast();
            forecast.setCity(city);
            forecast.setForecastDate(forecastDate);
            forecast.setTempMax((float) day.path("tempMax").asDouble());
            forecast.setTempMin((float) day.path("tempMin").asDouble());
            forecast.setWeatherTextDay(WeatherTextUtil.toChinese(day.path("textDay").asText()));
            forecast.setWeatherTextNight(WeatherTextUtil.toChinese(day.path("textNight").asText()));
            forecast.setHumidity((float) day.path("humidity").asDouble());
            forecast.setWindSpeed((float) day.path("windSpeedDay").asDouble());
            forecastMapper.insert(forecast);
            result.add(forecast);
        }
        // 预报数据向量化到 Qdrant，供 RAG 检索
        try {
            vectorizeForecasts(city);
        } catch (Exception e) {
            log.error("预报向量化失败: city={}, error={}", city, e.getMessage());
        }
        return result;
    }

    /**
     * 将预报数据向量化到 Qdrant
     */
    private void vectorizeForecasts(String city) {
        List<WeatherForecast> forecasts = forecastMapper.selectList(
                new LambdaQueryWrapper<WeatherForecast>()
                        .eq(WeatherForecast::getCity, city)
                        .ge(WeatherForecast::getForecastDate, LocalDate.now())
                        .orderByAsc(WeatherForecast::getForecastDate)
        );
        if (forecasts.isEmpty()) return;

        String reportId = "fcst_" + city;
        List<String> chunks = new ArrayList<>();
        List<String> sections = new ArrayList<>();
        List<Float> tempMaxes = new ArrayList<>();
        List<Float> tempMins = new ArrayList<>();

        for (WeatherForecast f : forecasts) {
            String chunk = String.format(
                    "【%s %s 天气预报】白天：%s，最高 %.1f℃；夜间：%s，最低 %.1f℃。湿度 %.0f%%，风力 %.1f 级。",
                    city, f.getForecastDate(),
                    f.getWeatherTextDay(), f.getTempMax(),
                    f.getWeatherTextNight(), f.getTempMin(),
                    f.getHumidity(), f.getWindSpeed()
            );
            chunks.add(chunk);
            sections.add("预报-" + f.getForecastDate());
            tempMaxes.add(f.getTempMax() != null ? f.getTempMax() : 0f);
            tempMins.add(f.getTempMin() != null ? f.getTempMin() : 0f);
        }

        List<float[]> vectors = embeddingService.embedBatch(chunks);

        // 清理旧预报向量，写入最新预报
        retrieverService.deleteByReportId(reportId);
        retrieverService.upsertChunks(
                reportId, chunks, vectors, city,
                LocalDate.now().toString(), 4, sections,
                tempMaxes, tempMins, "预报"
        );
        log.info("预报向量化完成: city={}, days={}", city, forecasts.size());
    }

    public Map<String, Object> fetchAllForUser(Long userId) {
        Set<String> cities = getAllFollowedCities();

        int nowCount = 0;
        int forecastCount = 0;
        List<String> errors = new ArrayList<>();

        for (String city : cities) {
            try {
                fetchNowForCity(city);
                nowCount++;
            } catch (Exception e) {
                errors.add(city + "实时天气: " + e.getMessage());
            }
            try {
                List<WeatherForecast> forecasts = fetchForecastForCity(city);
                forecastCount += forecasts.size();
            } catch (Exception e) {
                errors.add(city + "预报: " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cities", cities.size());
        result.put("nowFetched", nowCount);
        result.put("forecastFetched", forecastCount);
        result.put("errors", errors);
        return result;
    }

    public WeatherData getLatestNow(String city) {
        LambdaQueryWrapper<WeatherData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeatherData::getCity, city)
               .orderByDesc(WeatherData::getObsTime)
               .last("LIMIT 1");
        return weatherDataMapper.selectOne(wrapper);
    }

    public List<WeatherForecast> getForecast(String city) {
        LambdaQueryWrapper<WeatherForecast> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeatherForecast::getCity, city)
               .ge(WeatherForecast::getForecastDate, LocalDate.now())
               .orderByAsc(WeatherForecast::getForecastDate);
        return forecastMapper.selectList(wrapper);
    }

    public List<WeatherData> getHistory(String city, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<WeatherData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeatherData::getCity, city)
               .ge(WeatherData::getObsTime, since)
               .orderByAsc(WeatherData::getObsTime);
        return weatherDataMapper.selectList(wrapper);
    }

    public List<Map<String, Object>> getOverview(Long userId) {
        Set<String> cities = getAllFollowedCities();

        List<Map<String, Object>> result = new ArrayList<>();
        for (String cityName : cities) {
            WeatherData latest = getLatestNow(cityName);
            Map<String, Object> item = new HashMap<>();
            item.put("city", cityName);
            if (latest != null) {
                item.put("temp", latest.getTemp());
                item.put("feelsLike", latest.getFeelsLike());
                item.put("humidity", latest.getHumidity());
                item.put("weatherText", latest.getWeatherText());
                item.put("obsTime", latest.getObsTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
            }
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getStatistics(String city, int days) {
        List<WeatherData> history = getHistory(city, days);
        List<WeatherForecast> forecast = getForecast(city);

        Map<String, Object> result = new HashMap<>();
        result.put("recordCount", history.size());

        if (history.isEmpty()) {
            result.put("avgTemp", null);
            result.put("maxTemp", null);
            result.put("minTemp", null);
            result.put("trend", Collections.emptyList());
            result.put("dailySummary", Collections.emptyList());
            result.put("forecast", forecast);
            return result;
        }

        double avgTemp = history.stream().mapToDouble(w -> w.getTemp()).average().orElse(0);
        double maxTemp = history.stream().mapToDouble(w -> w.getTemp()).max().orElse(0);
        double minTemp = history.stream().mapToDouble(w -> w.getTemp()).min().orElse(0);

        result.put("avgTemp", Math.round(avgTemp * 10) / 10.0);
        result.put("maxTemp", Math.round(maxTemp * 10) / 10.0);
        result.put("minTemp", Math.round(minTemp * 10) / 10.0);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (WeatherData w : history) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", w.getObsTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
            point.put("temp", w.getTemp());
            point.put("feelsLike", w.getFeelsLike());
            point.put("humidity", w.getHumidity());
            point.put("windSpeed", w.getWindSpeed());
            trend.add(point);
        }
        result.put("trend", trend);

        Map<LocalDate, double[]> dailyMap = new LinkedHashMap<>();
        for (WeatherData w : history) {
            LocalDate d = w.getObsTime().toLocalDate();
            dailyMap.compute(d, (k, v) -> {
                if (v == null) return new double[]{w.getTemp(), w.getTemp()};
                v[0] = Math.max(v[0], w.getTemp());
                v[1] = Math.min(v[1], w.getTemp());
                return v;
            });
        }
        List<Map<String, Object>> dailySummary = new ArrayList<>();
        for (Map.Entry<LocalDate, double[]> e : dailyMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", e.getKey().format(DateTimeFormatter.ofPattern("MM-dd")));
            item.put("maxTemp", Math.round(e.getValue()[0] * 10) / 10.0);
            item.put("minTemp", Math.round(e.getValue()[1] * 10) / 10.0);
            dailySummary.add(item);
        }
        result.put("dailySummary", dailySummary);
        result.put("forecast", forecast);
        return result;
    }

    public Map<String, Object> getCompare(String cityA, String cityB, int days) {
        Map<String, Object> statsA = getStatistics(cityA, days);
        Map<String, Object> statsB = getStatistics(cityB, days);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> a = new HashMap<>();
        a.put("name", cityA);
        a.put("avgTemp", statsA.get("avgTemp"));
        a.put("maxTemp", statsA.get("maxTemp"));
        a.put("minTemp", statsA.get("minTemp"));
        a.put("recordCount", statsA.get("recordCount"));
        a.put("trend", statsA.get("trend"));
        a.put("dailySummary", statsA.get("dailySummary"));
        result.put("cityA", a);

        Map<String, Object> b = new HashMap<>();
        b.put("name", cityB);
        b.put("avgTemp", statsB.get("avgTemp"));
        b.put("maxTemp", statsB.get("maxTemp"));
        b.put("minTemp", statsB.get("minTemp"));
        b.put("recordCount", statsB.get("recordCount"));
        b.put("trend", statsB.get("trend"));
        b.put("dailySummary", statsB.get("dailySummary"));
        result.put("cityB", b);

        List<Map<String, Object>> diff = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trendA = (List<Map<String, Object>>) statsA.get("trend");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trendB = (List<Map<String, Object>>) statsB.get("trend");
        int size = Math.min(trendA.size(), trendB.size());
        for (int i = 0; i < size; i++) {
            Map<String, Object> d = new HashMap<>();
            d.put("time", trendA.get(i).get("time"));
            double tA = ((Number) trendA.get(i).get("temp")).doubleValue();
            double tB = ((Number) trendB.get(i).get("temp")).doubleValue();
            d.put("tempDiff", Math.round((tA - tB) * 10) / 10.0);
            double hA = ((Number) trendA.get(i).get("humidity")).doubleValue();
            double hB = ((Number) trendB.get(i).get("humidity")).doubleValue();
            d.put("humidityDiff", Math.round((hA - hB) * 10) / 10.0);
            double wA = ((Number) trendA.get(i).get("windSpeed")).doubleValue();
            double wB = ((Number) trendB.get(i).get("windSpeed")).doubleValue();
            d.put("windSpeedDiff", Math.round((wA - wB) * 10) / 10.0);
            diff.add(d);
        }
        result.put("diff", diff);
        return result;
    }

    public Map<String, Object> fetchForCity(String city) {
        int nowCount = 0;
        int forecastCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            fetchNowForCity(city);
            nowCount = 1;
        } catch (Exception e) {
            errors.add(city + "实时天气: " + e.getMessage());
        }
        try {
            List<WeatherForecast> forecasts = fetchForecastForCity(city);
            forecastCount = forecasts.size();
        } catch (Exception e) {
            errors.add(city + "预报: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nowFetched", nowCount);
        result.put("forecastFetched", forecastCount);
        result.put("errors", errors);
        return result;
    }

    public Set<String> getAllFollowedCities() {
        List<FollowedCity> all = followedCityMapper.selectList(null);
        return all.stream().map(FollowedCity::getCity).collect(Collectors.toSet());
    }
}
