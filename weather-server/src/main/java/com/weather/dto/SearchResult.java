package com.weather.dto;

import lombok.Data;

@Data
public class SearchResult {
    private String reportId;
    private int chunkIndex;
    private String city;
    private String date;
    private String section;
    private String content;
    private float score;
    private Float tempMax;
    private Float tempMin;
    private String weatherText;
}
