package com.weather.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private String city;
    private DateRange dateRange;
    private boolean stream = true;

    @Data
    public static class DateRange {
        private String start;
        private String end;
    }
}
