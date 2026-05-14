package com.weather.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Configuration
public class QdrantConfig {

    private static final Logger log = LoggerFactory.getLogger(QdrantConfig.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6333}")
    private int port;

    @Value("${qdrant.api-key:}")
    private String apiKey;

    @Value("${qdrant.collection:weather_reports}")
    private String collectionName;

    @Value("${embedding.dimension:1024}")
    private int vectorSize;

    public QdrantConfig(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String getBaseUrl() {
        return "http://" + host + ":" + port;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public int getVectorSize() {
        return vectorSize;
    }

    public HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("api-key", apiKey);
        }
        return headers;
    }

    @PostConstruct
    public void initCollection() {
        try {
            String url = getBaseUrl() + "/collections/" + collectionName;
            HttpEntity<?> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                log.info("Qdrant collection '{}' 已存在", collectionName);
                return;
            }
        } catch (Exception e) {
            log.info("Qdrant collection '{}' 不存在，开始创建...", collectionName);
        }

        try {
            String url = getBaseUrl() + "/collections/" + collectionName;
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode vectors = body.putObject("vectors");
            vectors.put("size", vectorSize);
            vectors.put("distance", "Cosine");

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), buildHeaders());
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            log.info("Qdrant collection '{}' 创建成功 (维度={}, 距离=Cosine)", collectionName, vectorSize);

            // 创建 Payload 索引
            createPayloadIndex("city", "keyword");
            createPayloadIndex("date", "datetime");
            createPayloadIndex("report_type", "keyword");
        } catch (Exception e) {
            log.error("Qdrant collection 初始化失败: {}", e.getMessage());
        }
    }

    private void createPayloadIndex(String field, String type) {
        try {
            String url = getBaseUrl() + "/collections/" + collectionName + "/index";
            ObjectNode body = objectMapper.createObjectNode();
            body.put("field_name", field);
            body.put("field_schema", type);
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), buildHeaders());
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        } catch (Exception e) {
            log.warn("创建索引失败: field={}, type={}, error={}", field, type, e.getMessage());
        }
    }
}
