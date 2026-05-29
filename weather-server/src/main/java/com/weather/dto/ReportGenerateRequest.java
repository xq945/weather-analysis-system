package com.weather.dto;

import lombok.Data;

/**
 * 报告生成请求参数
 */
@Data
public class ReportGenerateRequest {
    private String city;            // 城市
    private String date;            // 日期 yyyy-MM-dd
    private Integer reportType = 1; // 类型: 1=日报, 2=周报, 3=深度分析
}
