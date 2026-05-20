package com.weather.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.dto.ChatRequest;
import com.weather.dto.ChatResponse;
import com.weather.service.ChatService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    private static final ObjectMapper JSON = new ObjectMapper();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat/ask")
    public SseEmitter ask(@RequestBody ChatRequest req) {
        if (req.getQuestion() == null || req.getQuestion().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
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
            // 非流式走 SseEmitter 不太合适，但这里保持统一
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
        return chatService.answer(req);
    }
}
