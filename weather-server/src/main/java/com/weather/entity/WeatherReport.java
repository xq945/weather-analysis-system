package com.weather.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 天气分析报告（AI 生成，同时也是 RAG 的数据源）
 */
@Data
@TableName("weather_report")
public class WeatherReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportId;        // 业务 ID，如 rpt_20260513_beijing_001
    private String city;
    private LocalDate reportDate;
    private Integer reportType;     // 类型: 1=日报, 2=周报, 3=深度分析
    private String title;
    private String content;         // 报告原文（Markdown）
    private Integer chunkCount;     // 向量分片数量
    private Integer qdrantSynced;   // 是否已同步向量库: 0=未同步, 1=已同步
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
