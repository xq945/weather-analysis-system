package com.weather.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponse {
    private String content;
    private List<SourceInfo> sources;

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
