package com.weather.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.dto.ChatRequest;
import com.weather.dto.ChatResponse;
import com.weather.service.ChatService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * AI 智能问答接口（SSE 流式 + 非流式）
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    private static final ObjectMapper JSON = new ObjectMapper();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * AI 问答入口
     *
     * 流式模式下返回 SseEmitter，逐字推送 delta 事件；
     * 非流式模式下后端仍走 SSE 协议，但前端收齐后一次性展示。
     *
     * @param req 包含问题、城市、时间范围、是否流式等参数
     * @return SSE 流式响应
     */
    @PostMapping("/chat/ask")
    public SseEmitter ask(@RequestBody ChatRequest req) {
        if (req.getQuestion() == null || req.getQuestion().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        // 校验日期格式（如果有传入）
        if (req.getDateRange() != null) {
            try {
                if (req.getDateRange().getStart() != null) {
                    LocalDate.parse(req.getDateRange().getStart());
                }
                if (req.getDateRange().getEnd() != null) {
                    LocalDate.parse(req.getDateRange().getEnd());
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("日期格式错误，应为 yyyy-MM-dd");
            }
        }
        if (!req.isStream()) {
            // 非流式：先获取完整回答，再封装成单个 delta 事件 + done 事件返回
            ChatResponse resp = chatService.answerSync(req);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("delta").data(resp.getContent()));
                String sourcesJson = resp.getSources() != null ?
                        JSON.writeValueAsString(resp.getSources()) : "[]";
                emitter.send(SseEmitter.event().name("done").data(sourcesJson));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        // 流式：交由 ChatService 异步处理，通过 SSE 逐字推送
        return chatService.answer(req);
    }
}
