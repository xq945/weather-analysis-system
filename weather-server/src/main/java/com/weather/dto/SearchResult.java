package com.weather.dto;

import lombok.Data;

/**
 * Qdrant 向量检索结果
 */
@Data
public class SearchResult {
    private String reportId;    // 报告 ID
    private int chunkIndex;     // Chunk 索引
    private String city;
    private String date;
    private String section;     // 段落名称
    private String content;     // 文本片段
    private float score;        // 向量相似度分数
    private Float tempMax;
    private Float tempMin;
    private String weatherText;
}
