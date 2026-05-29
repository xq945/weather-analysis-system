package com.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.weather.config.QdrantConfig;
import com.weather.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Qdrant 向量检索服务：向量搜索、写入 Chunk、按 report_id 删除
 */
@Service
public class RetrieverService {

    private static final Logger log = LoggerFactory.getLogger(RetrieverService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final QdrantConfig qdrantConfig;

    public RetrieverService(RestTemplate restTemplate, ObjectMapper objectMapper, QdrantConfig qdrantConfig) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.qdrantConfig = qdrantConfig;
    }

    /**
     * 向量检索 + 元数据过滤
     */
    public List<SearchResult> search(float[] queryVector, String city, LocalDate dateFrom, LocalDate dateTo, int topK) {
        try {
            var body = objectMapper.createObjectNode();
            ArrayNode vectorArray = body.putArray("vector");
            for (float v : queryVector) {
                vectorArray.add(v);
            }
            body.put("limit", topK);
            body.put("score_threshold", 0.6);
            body.put("with_payload", true);

            // filter
            var filter = body.putObject("filter");
            var must = filter.putArray("must");

            // city filter
            var cityFilter = objectMapper.createObjectNode();
            cityFilter.put("key", "city");
            var cityMatch = cityFilter.putObject("match");
            cityMatch.put("value", city);
            must.add(cityFilter);

            // date range filter
            if (dateFrom != null || dateTo != null) {
                var dateFilter = objectMapper.createObjectNode();
                dateFilter.put("key", "date");
                var range = dateFilter.putObject("range");
                if (dateFrom != null) {
                    range.put("gte", dateFrom.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toString());
                }
                if (dateTo != null) {
                    range.put("lte", dateTo.atTime(23, 59, 59).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toString());
                }
                must.add(dateFilter);
            }

            String url = qdrantConfig.getBaseUrl() + "/collections/" + qdrantConfig.getCollectionName() + "/points/search";
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), qdrantConfig.buildHeaders());
            ResponseEntity<JsonNode> resp = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

            List<SearchResult> results = new ArrayList<>();
            JsonNode root = resp.getBody();
            if (root != null && root.has("result")) {
                for (JsonNode point : root.get("result")) {
                    SearchResult r = new SearchResult();
                    r.setScore((float) point.get("score").asDouble());

                    JsonNode payload = point.get("payload");
                    if (payload != null) {
                        r.setReportId(getStr(payload, "report_id"));
                        r.setChunkIndex(getInt(payload, "chunk_index"));
                        r.setCity(getStr(payload, "city"));
                        r.setDate(getStr(payload, "date"));
                        r.setSection(getStr(payload, "section"));
                        r.setContent(getStr(payload, "content"));
                        r.setTempMax(getFloat(payload, "temp_max"));
                        r.setTempMin(getFloat(payload, "temp_min"));
                        r.setWeatherText(getStr(payload, "weather_text"));
                    }
                    results.add(r);
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Qdrant 检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 写入 Chunk 向量
     */
    public void upsertChunks(String reportId, java.util.List<String> chunks, java.util.List<float[]> vectors,
                             String city, String date, int reportType,
                             java.util.List<String> sections, java.util.List<Float> tempMaxes,
                             java.util.List<Float> tempMins, String weatherText) {
        try {
            var body = objectMapper.createObjectNode();
            var points = body.putArray("points");

            for (int i = 0; i < chunks.size(); i++) {
                var point = objectMapper.createObjectNode();
                point.put("id", UUID.nameUUIDFromBytes((reportId + "_" + i).getBytes()).toString());

                ArrayNode vec = point.putArray("vector");
                for (float v : vectors.get(i)) {
                    vec.add(v);
                }

                var payload = point.putObject("payload");
                payload.put("report_id", reportId);
                payload.put("chunk_index", i);
                payload.put("city", city);
                payload.put("date", date);
                payload.put("report_type", reportType);
                payload.put("section", sections.get(i));
                payload.put("content", chunks.get(i));
                payload.put("temp_max", tempMaxes.get(i) != null ? tempMaxes.get(i) : 0f);
                payload.put("temp_min", tempMins.get(i) != null ? tempMins.get(i) : 0f);
                payload.put("weather_text", weatherText != null ? weatherText : "");

                points.add(point);
            }

            String url = qdrantConfig.getBaseUrl() + "/collections/" + qdrantConfig.getCollectionName() + "/points";
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), qdrantConfig.buildHeaders());
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            log.info("Qdrant upsert 成功: reportId={}, chunks={}", reportId, chunks.size());
        } catch (Exception e) {
            log.error("Qdrant upsert 失败: {}", e.getMessage());
            throw new RuntimeException("Qdrant 写入失败: " + e.getMessage());
        }
    }

    /**
     * 按 report_id 删除向量
     */
    public void deleteByReportId(String reportId) {
        try {
            String url = qdrantConfig.getBaseUrl() + "/collections/" + qdrantConfig.getCollectionName() + "/points/delete";
            var body = objectMapper.createObjectNode();
            var filter = body.putObject("filter");
            var must = filter.putArray("must");
            var cond = objectMapper.createObjectNode();
            cond.put("key", "report_id");
            var match = cond.putObject("match");
            match.put("value", reportId);
            must.add(cond);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), qdrantConfig.buildHeaders());
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("Qdrant 删除成功: reportId={}", reportId);
        } catch (Exception e) {
            log.warn("Qdrant 删除失败: reportId={}, error={}", reportId, e.getMessage());
        }
    }

    private String getStr(JsonNode node, String key) {
        JsonNode val = node.get(key);
        return val != null && !val.isNull() ? val.asText() : "";
    }

    private int getInt(JsonNode node, String key) {
        JsonNode val = node.get(key);
        return val != null && !val.isNull() ? val.asInt() : 0;
    }

    private Float getFloat(JsonNode node, String key) {
        JsonNode val = node.get(key);
        return val != null && !val.isNull() ? (float) val.asDouble() : null;
    }
}
