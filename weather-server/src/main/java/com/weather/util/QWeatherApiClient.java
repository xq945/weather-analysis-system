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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 和风天气 API 客户端：城市搜索、实时天气、7 天预报
 *
 * 支持 GZIP 解压，城市代码自动缓存到本地表，减少 API 调用。
 */
@Component
public class QWeatherApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CityListMapper cityListMapper;
    private final String apiKey;
    private final String baseUrl;

    /** 城市代码内存缓存，避免重复查询 */
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

    /**
     * 调用和风 API，自动处理 GZIP 压缩响应
     *
     * 和风天气的响应默认使用 GZIP 压缩，通过检查前两个字节的魔数（0x1F 0x8B）
     * 判断是否需要解压。
     */
    private JsonNode callApi(URI uri) {
        HttpEntity<?> entity = new HttpEntity<>(buildHeaders());
        ResponseEntity<byte[]> resp = restTemplate.exchange(uri, HttpMethod.GET, entity, byte[].class);
        try {
            byte[] body = resp.getBody();
            // 检测 GZIP 魔数
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

    /**
     * 搜索城市并获取城市代码
     *
     * 查询顺序：内存缓存 → 本地 city_list 表 → 和风 geo API
     * 从 API 查询到的结果会自动写入本地表和内存缓存。
     *
     * @param cityName 城市中文名
     * @return 和风天气城市代码
     */
    public String searchCity(String cityName) {
        // 1. 内存缓存
        if (cityCache.containsKey(cityName)) {
            return cityCache.get(cityName);
        }
        // 2. 本地数据库
        LambdaQueryWrapper<CityList> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CityList::getCityName, cityName);
        CityList city = cityListMapper.selectOne(wrapper);
        if (city != null) {
            cityCache.put(cityName, city.getCityCode());
            return city.getCityCode();
        }
        // 3. 远程 API
        URI url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/geo/v2/city/lookup")
                .queryParam("location", cityName)
                .build()
                .encode()
                .toUri();
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

        // 写入本地表
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

    /** 获取指定城市的实时天气 */
    public JsonNode getWeatherNow(String cityId) {
        URI url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/v7/weather/now")
                .queryParam("location", cityId)
                .queryParam("lang", "zh")
                .build()
                .encode()
                .toUri();
        JsonNode root = callApi(url);
        if (!"200".equals(root.path("code").asText())) {
            throw new RuntimeException("获取实时天气失败 code=" + root.path("code").asText());
        }
        return root.path("now");
    }

    /** 获取指定城市 7 天预报 */
    public JsonNode getWeather7d(String cityId) {
        URI url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/v7/weather/7d")
                .queryParam("location", cityId)
                .queryParam("lang", "zh")
                .build()
                .encode()
                .toUri();
        JsonNode root = callApi(url);
        if (!"200".equals(root.path("code").asText())) {
            throw new RuntimeException("获取7天预报失败 code=" + root.path("code").asText());
        }
        return root.path("daily");
    }
}
