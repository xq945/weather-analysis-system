package com.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * LLM 调用服务：对接 DeepSeek API，支持流式（SSE）和非流式两种模式
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final Executor asyncExecutor;
    private final OkHttpClient httpClient;

    public LlmService(ObjectMapper objectMapper,
                      OkHttpClient okHttpClient,
                      Executor asyncExecutor,
                      @Value("${llm.api-key}") String apiKey,
                      @Value("${llm.base-url:https://api.deepseek.com}") String baseUrl,
                      @Value("${llm.model:deepseek-chat}") String model,
                      @Value("${llm.max-tokens:1000}") int maxTokens,
                      @Value("${llm.temperature:0.3}") double temperature) {
        this.objectMapper = objectMapper;
        this.httpClient = okHttpClient;
        this.asyncExecutor = asyncExecutor;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    /**
     * 流式调用 DeepSeek API
     *
     * 在异步线程中发起 SSE 请求，逐字解析 delta 事件并发送到外部 emitter。
     * 流结束后通过 onDone 回调通知 ChatService 发送 sources 信息。
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户问题
     * @param emitter      由 ChatService 创建的 SseEmitter
     * @param onDone       LLM 流结束后回调（发送 done 事件 + 引用来源）
     */
    public void chatStream(String systemPrompt, String userMessage, SseEmitter emitter, Runnable onDone) {
        asyncExecutor.execute(() -> {
            try {
                // 构建 Chat Completion 请求
                ObjectNode body = objectMapper.createObjectNode();
                body.put("model", model);
                body.put("max_tokens", maxTokens);
                body.put("temperature", temperature);
                body.put("stream", true);

                ArrayNode messages = body.putArray("messages");
                ObjectNode sysMsg = objectMapper.createObjectNode();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);

                ObjectNode userMsg = objectMapper.createObjectNode();
                userMsg.put("role", "user");
                userMsg.put("content", userMessage);
                messages.add(userMsg);

                // 使用 OkHttp 发起 SSE 请求
                Request request = new Request.Builder()
                        .url(baseUrl + "/v1/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                        .build();

                EventSource eventSource = EventSources.createFactory(httpClient)
                        .newEventSource(request, new EventSourceListener() {
                            @Override
                            public void onEvent(EventSource es, String id, String type, String data) {
                                try {
                                    // 结束标记
                                    if (data == null || "[DONE]".equals(data.trim())) {
                                        return;
                                    }
                                    var node = objectMapper.readTree(data);
                                    var choices = node.get("choices");
                                    if (choices != null && choices.size() > 0) {
                                        var delta = choices.get(0).get("delta");
                                        if (delta != null) {
                                            var content = delta.get("content");
                                            // 将增量文本通过 SSE 推送到前端
                                            if (content != null && !content.isNull()) {
                                                emitter.send(SseEmitter.event()
                                                        .name("delta")
                                                        .data(content.asText()));
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("SSE 解析失败: {}", e.getMessage());
                                }
                            }

                            @Override
                            public void onClosed(EventSource es) {
                                // 流正常结束，触发 ChatService 的回调发送 done 事件
                                if (onDone != null) {
                                    onDone.run();
                                }
                            }

                            @Override
                            public void onFailure(EventSource es, Throwable t, okhttp3.Response response) {
                                log.error("LLM SSE 连接失败: {}", t != null ? t.getMessage() : "unknown");
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("delta")
                                            .data("抱歉，处理您的请求时出现错误，请稍后重试。"));
                                    emitter.send(SseEmitter.event().name("done").data("[]"));
                                } catch (IOException e) {
                                    // emitter 已断开，无需处理
                                }
                                emitter.completeWithError(t != null ? t : new RuntimeException("LLM 连接失败"));
                            }
                        });

                // SseEmitter 超时或完成后自动取消 SSE 连接
                emitter.onCompletion(eventSource::cancel);
                emitter.onTimeout(eventSource::cancel);

            } catch (Exception e) {
                log.error("LLM 流式调用失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("delta")
                            .data("抱歉，处理您的请求时出现错误，请稍后重试。"));
                    emitter.send(SseEmitter.event().name("done").data("[]"));
                } catch (IOException ex) {
                    // emitter 已断开
                }
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * 非流式调用 DeepSeek API
     *
     * 同步等待完整响应后返回文本内容。
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户问题
     * @return LLM 返回的完整回答文本
     */
    public String chat(String systemPrompt, String userMessage) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("temperature", temperature);
            body.put("stream", false);

            ArrayNode messages = body.putArray("messages");
            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            Request request = new Request.Builder()
                    .url(baseUrl + "/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                var node = objectMapper.readTree(responseBody);
                var choices = node.get("choices");
                if (choices != null && choices.size() > 0) {
                    var msg = choices.get(0).get("message");
                    if (msg != null) {
                        var content = msg.get("content");
                        if (content != null && !content.isNull()) {
                            return content.asText();
                        }
                    }
                }
                throw new RuntimeException("LLM 返回格式异常: " + responseBody);
            }
        } catch (IOException e) {
            log.error("LLM API 调用失败: {}", e.getMessage());
            throw new RuntimeException("LLM 调用失败: " + e.getMessage());
        }
    }
}
