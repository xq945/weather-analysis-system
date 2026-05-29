package com.weather.dto;

import lombok.Data;

/**
 * AI 问答请求参数
 */
@Data
public class ChatRequest {
    private String question;        // 用户问题
    private String city;            // 指定城市（可选，为空则自动提取）
    private DateRange dateRange;    // 时间范围（可选）
    private boolean stream = true;  // 是否流式输出 SSE，默认 true

    @Data
    public static class DateRange {
        private String start;   // 开始日期 yyyy-MM-dd
        private String end;     // 结束日期 yyyy-MM-dd
    }
}
