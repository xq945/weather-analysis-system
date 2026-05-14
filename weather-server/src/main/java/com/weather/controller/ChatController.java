package com.weather.controller;

import com.weather.dto.ChatRequest;
import com.weather.dto.ChatResponse;
import com.weather.dto.Result;
import com.weather.service.ChatService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat/ask")
    public SseEmitter ask(@RequestBody ChatRequest req) {
        if (req.getQuestion() == null || req.getQuestion().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        if (!req.isStream()) {
            // 非流式走 SseEmitter 不太合适，但这里保持统一
            ChatResponse resp = chatService.answerSync(req);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("delta").data(resp.getContent()));
                StringBuilder srcJson = new StringBuilder("[");
                if (resp.getSources() != null) {
                    for (int i = 0; i < resp.getSources().size(); i++) {
                        if (i > 0) srcJson.append(",");
                        ChatResponse.SourceInfo s = resp.getSources().get(i);
                        srcJson.append(String.format(
                                "{\"city\":\"%s\",\"date\":\"%s\",\"section\":\"%s\"}",
                                s.getCity(), s.getDate(), s.getSection()));
                    }
                }
                srcJson.append("]");
                emitter.send(SseEmitter.event().name("done").data(srcJson.toString()));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        return chatService.answer(req);
    }
}
