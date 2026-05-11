package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("weather_forecast")
public class WeatherForecast {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String city;
    private LocalDate forecastDate;
    private Float tempMax;
    private Float tempMin;
    private String weatherTextDay;
    private String weatherTextNight;
    private Float humidity;
    private Float windSpeed;
    private LocalDateTime createTime;
}
