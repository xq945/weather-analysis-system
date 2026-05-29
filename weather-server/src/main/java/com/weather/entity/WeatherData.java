package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实时天气数据（由定时任务或手动拉取写入）
 */
@Data
@TableName("weather_data")
public class WeatherData {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String city;
    private LocalDateTime obsTime;      // 观测时间
    private Float temp;                 // 温度 (℃)
    private Float feelsLike;            // 体感温度 (℃)
    private Float humidity;             // 相对湿度 (%)
    private Float windSpeed;            // 风速 (级)
    private Float pressure;             // 气压 (hPa)
    private Float visibility;           // 能见度 (km)
    private String weatherText;         // 天气现象文字描述
    private LocalDateTime createTime;   // 记录写入时间
}
