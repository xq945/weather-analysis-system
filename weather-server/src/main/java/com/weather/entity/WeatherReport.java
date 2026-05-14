package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("weather_report")
public class WeatherReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportId;
    private String city;
    private LocalDate reportDate;
    private Integer reportType;
    private String title;
    private String content;
    private Integer chunkCount;
    private Integer qdrantSynced;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
