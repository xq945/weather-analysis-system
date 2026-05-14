package com.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int dimension;
    private final int batchSize;

    public EmbeddingService(RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            @Value("${embedding.api-key}") String apiKey,
                            @Value("${embedding.base-url:https://api.deepseek.com}") String baseUrl,
                            @Value("${embedding.model:deepseek-chat}") String model,
                            @Value("${embedding.dimension:1024}") int dimension,
                            @Value("${embedding.batch-size:20}") int batchSize) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.dimension = dimension;
        this.batchSize = batchSize;
    }

    public float[] embed(String text) {
        List<float[]> results = embedBatch(Collections.singletonList(text));
        if (results.isEmpty()) {
            throw new RuntimeException("Embedding 返回空结果");
        }
        return results.get(0);
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<float[]> allVectors = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);

                ObjectNode body = objectMapper.createObjectNode();
                body.put("model", model);
                ArrayNode input = body.putArray("input");
                for (String t : batch) {
                    input.add(t);
                }

                HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

                String url = baseUrl + "/v1/embeddings";
                ResponseEntity<JsonNode> resp = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

                JsonNode root = resp.getBody();
                if (root != null && root.has("data")) {
                    JsonNode data = root.get("data");
                    if (data.isArray()) {
                        for (JsonNode item : data) {
                            JsonNode emb = item.get("embedding");
                            if (emb != null && emb.isArray()) {
                                float[] vec = new float[emb.size()];
                                for (int j = 0; j < emb.size(); j++) {
                                    vec[j] = (float) emb.get(j).asDouble();
                                }
                                allVectors.add(vec);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Embedding API 调用失败: {}", e.getMessage());
                throw new RuntimeException("Embedding 调用失败: " + e.getMessage());
            }
        }

        return allVectors;
    }

    public int getDimension() {
        return dimension;
    }
}
