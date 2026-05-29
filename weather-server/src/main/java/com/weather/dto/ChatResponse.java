package com.weather.dto;

import lombok.Data;
import java.util.List;

/**
 * AI 问答非流式响应
 */
@Data
public class ChatResponse {
    private String content;               // LLM 回答文本
    private List<SourceInfo> sources;     // 引用来源

    @Data
    public static class SourceInfo {
        private String city;
        private String date;
        private String section;

        public SourceInfo(String city, String date, String section) {
            this.city = city;
            this.date = date;
            this.section = section;
        }
    }
}
