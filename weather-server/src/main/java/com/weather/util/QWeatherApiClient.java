package com.weather.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.entity.CityList;
import com.weather.mapper.CityListMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Component
public class QWeatherApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CityListMapper cityListMapper;
    private final String apiKey;
    private final String baseUrl;

    private final Map<String, String> cityCache = new HashMap<>();

    public QWeatherApiClient(RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             CityListMapper cityListMapper,
                             @Value("${qweather.api-key}") String apiKey,
                             @Value("${qweather.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.cityListMapper = cityListMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-QW-Api-Key", apiKey);
        return headers;
    }

    private JsonNode callApi(String url) {
        HttpEntity<?> entity = new HttpEntity<>(buildHeaders());
        ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        try {
            byte[] body = resp.getBody();
            if (body != null && body.length >= 2 && body[0] == (byte) 0x1F && body[1] == (byte) 0x8B) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(body));
                byte[] buf = new byte[4096];
                int n;
                while ((n = gzis.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                gzis.close();
                return objectMapper.readTree(out.toByteArray());
            }
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("API调用失败: " + e.getMessage());
        }
    }

    public String searchCity(String cityName) {
        if (cityCache.containsKey(cityName)) {
            return cityCache.get(cityName);
        }
        // 先查本地表
        LambdaQueryWrapper<CityList> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CityList::getCityName, cityName);
        CityList city = cityListMapper.selectOne(wrapper);
        if (city != null) {
            cityCache.put(cityName, city.getCityCode());
            return city.getCityCode();
        }
        // 本地没有，调 geo API
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/geo/v2/city/lookup")
                .queryParam("location", cityName)
                .toUriString();
        JsonNode root = callApi(url);
        String code = root.path("code").asText();
        if (!"200".equals(code)) {
            throw new RuntimeException("城市搜索失败: " + cityName + " code=" + code);
        }
        JsonNode location = root.path("location");
        if (location.isEmpty()) {
            throw new RuntimeException("未找到城市: " + cityName);
        }
        String cityId = location.get(0).path("id").asText();
        String province = location.get(0).path("adm1").asText("");
        cityCache.put(cityName, cityId);

        // 写入本地表，下次直接查表
        CityList newCity = new CityList();
        newCity.setCityName(cityName);
        newCity.setCityCode(cityId);
        newCity.setProvince(province.isEmpty() ? null : province);
        try {
            cityListMapper.insert(newCity);
        } catch (Exception ignored) {
            // 唯一键冲突说明已有，忽略
        }
        return cityId;
    }

    public JsonNode getWeatherNow(String cityId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/v7/weather/now")
                .queryParam("location", cityId)
                .toUriString();
        JsonNode root = callApi(url);
        if (!"200".equals(root.path("code").asText())) {
            throw new RuntimeException("获取实时天气失败 code=" + root.path("code").asText());
        }
        return root.path("now");
    }

    public JsonNode getWeather7d(String cityId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/v7/weather/7d")
                .queryParam("location", cityId)
                .toUriString();
        JsonNode root = callApi(url);
        if (!"200".equals(root.path("code").asText())) {
            throw new RuntimeException("获取7天预报失败 code=" + root.path("code").asText());
        }
        return root.path("daily");
    }
}
