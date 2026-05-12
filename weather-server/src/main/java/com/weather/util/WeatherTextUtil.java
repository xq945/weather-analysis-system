package com.weather.util;

import java.util.HashMap;
import java.util.Map;

public final class WeatherTextUtil {

    private static final Map<String, String> EN_TO_ZH = new HashMap<>();

    static {
        EN_TO_ZH.put("Sunny", "晴");
        EN_TO_ZH.put("Clear", "晴");
        EN_TO_ZH.put("Few Clouds", "少云");
        EN_TO_ZH.put("Partly Cloudy", "多云");
        EN_TO_ZH.put("Partly Clear", "晴间多云");
        EN_TO_ZH.put("Cloudy", "阴");
        EN_TO_ZH.put("Overcast", "阴");
        EN_TO_ZH.put("Light Rain", "小雨");
        EN_TO_ZH.put("Moderate Rain", "中雨");
        EN_TO_ZH.put("Heavy Rain", "大雨");
        EN_TO_ZH.put("Rainstorm", "暴雨");
        EN_TO_ZH.put("Heavy Rainstorm", "大暴雨");
        EN_TO_ZH.put("Severe Rainstorm", "特大暴雨");
        EN_TO_ZH.put("Shower Rain", "阵雨");
        EN_TO_ZH.put("Thundershower", "雷阵雨");
        EN_TO_ZH.put("Thunderstorm", "雷阵雨");
        EN_TO_ZH.put("Light Snow", "小雪");
        EN_TO_ZH.put("Moderate Snow", "中雪");
        EN_TO_ZH.put("Heavy Snow", "大雪");
        EN_TO_ZH.put("Snowstorm", "暴雪");
        EN_TO_ZH.put("Sleet", "雨夹雪");
        EN_TO_ZH.put("Freezing Rain", "冻雨");
        EN_TO_ZH.put("Foggy", "雾");
        EN_TO_ZH.put("Fog", "雾");
        EN_TO_ZH.put("Haze", "霾");
        EN_TO_ZH.put("Blowing Sand", "扬沙");
        EN_TO_ZH.put("Sandstorm", "沙尘暴");
        EN_TO_ZH.put("Heavy Sandstorm", "强沙尘暴");
        EN_TO_ZH.put("Dust", "浮尘");
        EN_TO_ZH.put("Windy", "大风");
        EN_TO_ZH.put("Tropical Storm", "热带风暴");
    }

    public static String toChinese(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return EN_TO_ZH.getOrDefault(text, text);
    }

    private WeatherTextUtil() {}
}
