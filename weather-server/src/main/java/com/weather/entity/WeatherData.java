package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("weather_data")
public class WeatherData {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String city;
    private LocalDateTime obsTime;
    private Float temp;
    private Float feelsLike;
    private Float humidity;
    private Float windSpeed;
    private Float pressure;
    private Float visibility;
    private String weatherText;
    private LocalDateTime createTime;
}
