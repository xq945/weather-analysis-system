package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 天气预报数据（7 天预报）
 */
@Data
@TableName("weather_forecast")
public class WeatherForecast {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String city;
    private LocalDate forecastDate;     // 预报日期
    private Float tempMax;              // 最高温度
    private Float tempMin;              // 最低温度
    private String weatherTextDay;      // 白天天气现象
    private String weatherTextNight;    // 夜间天气现象
    private Float humidity;             // 相对湿度
    private Float windSpeed;            // 风速
    private LocalDateTime createTime;
}
