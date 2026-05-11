package com.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.weather.util.QWeatherApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class QWeatherApiTest {

    @Autowired
    private QWeatherApiClient apiClient;

    @Test
    public void testLondon() {
        // 搜索伦敦
        String cityId = apiClient.searchCity("伦敦");
        System.out.println("伦敦 cityId: " + cityId);

        // 实时天气
        JsonNode now = apiClient.getWeatherNow(cityId);
        System.out.println("=== 伦敦实时天气 ===");
        System.out.println("温度: " + now.path("temp").asText() + "°C");
        System.out.println("体感: " + now.path("feelsLike").asText() + "°C");
        System.out.println("湿度: " + now.path("humidity").asText() + "%");
        System.out.println("气压: " + now.path("pressure").asText() + " hPa");
        System.out.println("风速: " + now.path("windSpeed").asText() + " km/h");
        System.out.println("天气: " + now.path("text").asText());

        // 7天预报
        JsonNode daily = apiClient.getWeather7d(cityId);
        System.out.println("=== 伦敦7天预报 ===");
        for (JsonNode day : daily) {
            System.out.println(day.path("fxDate").asText()
                    + " | 最高:" + day.path("tempMax").asText() + "°C"
                    + " | 最低:" + day.path("tempMin").asText() + "°C"
                    + " | 白天:" + day.path("textDay").asText()
                    + " | 夜间:" + day.path("textNight").asText());
        }
    }
}
