package com.weather.dto;

import lombok.Data;

@Data
public class ReportGenerateRequest {
    private String city;
    private String date;
    private Integer reportType = 1;
}
