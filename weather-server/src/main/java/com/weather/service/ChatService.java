package com.weather.service;

import com.weather.dto.ChatRequest;
import com.weather.dto.ChatResponse;
import com.weather.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private String defaultSystemPrompt() {
        return "当前系统日期：" + LocalDate.now() + "。请根据此日期理解\"今天\"\"昨天\"\"明天\"等相对日期概念。\n"
             + "你是天气数据分析助手。请根据用户的问题给出简明准确的回答。";
    }

    private final EmbeddingService embeddingService;
    private final RetrieverService retrieverService;
    private final LlmService llmService;

    public ChatService(EmbeddingService embeddingService,
                       RetrieverService retrieverService,
                       LlmService llmService) {
        this.embeddingService = embeddingService;
        this.retrieverService = retrieverService;
        this.llmService = llmService;
    }

    /**
     * RAG 对话入口：流式
     * 流程：向量化 → Qdrant 检索 → 构建 Prompt → 调用 DeepSeek → SSE 返回
     * 如果检索不到结果，直接调用大模型回答
     */
    public SseEmitter answer(ChatRequest req) {
        String city = req.getCity();
        LocalDate dateFrom = null;
        LocalDate dateTo = null;

        if (req.getDateRange() != null) {
            if (req.getDateRange().getStart() != null) {
                dateFrom = LocalDate.parse(req.getDateRange().getStart());
            }
            if (req.getDateRange().getEnd() != null) {
                dateTo = LocalDate.parse(req.getDateRange().getEnd());
            }
        }
        if (dateFrom == null) dateFrom = LocalDate.now().minusDays(7);
        if (dateTo == null) dateTo = LocalDate.now();

        if (city == null || city.isEmpty()) {
            city = extractCity(req.getQuestion());
        }
        // lambda 中需要 effectively final 变量，创建副本
        final String finalCity = city;
        final LocalDate finalDateFrom = dateFrom;
        final LocalDate finalDateTo = dateTo;

        SseEmitter emitter = new SseEmitter(120000L);

        new Thread(() -> {
            try {
                String systemPrompt;
                List<ChatResponse.SourceInfo> sources = new ArrayList<>();

                // 如果识别到城市，执行 RAG 检索
                if (finalCity != null && !finalCity.isEmpty()) {
                    // 问题向量化
                    float[] questionVector = embeddingService.embed(req.getQuestion());

                    // Qdrant 向量检索
                    List<SearchResult> results = retrieverService.search(
                            questionVector, finalCity, finalDateFrom, finalDateTo, 10);

                    if (!results.isEmpty()) {
                        // 去重，同一报告同一段落只保留最高分
                        List<SearchResult> deduped = deduplicateByReportId(results);

                        // 构建带检索上下文的 Prompt
                        systemPrompt = buildSystemPrompt(deduped);

                        // 收集引用来源
                        for (SearchResult r : deduped) {
                            sources.add(new ChatResponse.SourceInfo(r.getCity(), r.getDate(), r.getSection()));
                        }
                    } else {
                        // 检索不到结果，直接调用大模型
                        log.info("Qdrant 未检索到 {} 的相关报告，直接调用 LLM", finalCity);
                        systemPrompt = defaultSystemPrompt();
                    }
                } else {
                    // 未识别到城市，直接调用大模型
                    log.info("未识别到城市，直接调用 LLM");
                    systemPrompt = defaultSystemPrompt();
                }

                // 嵌套 lambda 需要 effectively final 变量
                final String finalSystemPrompt = systemPrompt;
                final List<ChatResponse.SourceInfo> finalSources = sources;
                llmService.chatStream(finalSystemPrompt, req.getQuestion(), emitter, () -> {
                    try {
                        // LLM 流结束，发送 done 事件（含引用来源）
                        StringBuilder srcJson = new StringBuilder("[");
                        for (int i = 0; i < finalSources.size(); i++) {
                            if (i > 0) srcJson.append(",");
                            ChatResponse.SourceInfo s = finalSources.get(i);
                            srcJson.append(String.format(
                                    "{\"city\":\"%s\",\"date\":\"%s\",\"section\":\"%s\"}",
                                    s.getCity(), s.getDate(), s.getSection()));
                        }
                        srcJson.append("]");
                        emitter.send(SseEmitter.event().name("done").data(srcJson.toString()));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });

            } catch (Exception e) {
                log.error("RAG 流式处理失败: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("delta")
                            .data("抱歉，处理您的请求时出现错误，请稍后重试。"));
                    emitter.send(SseEmitter.event().name("done").data("[]"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(e);
                }
            }
        }).start();

        return emitter;
    }

    /**
     * 非流式回答
     */
    public ChatResponse answerSync(ChatRequest req) {
        try {
            String city = req.getCity();
            LocalDate dateFrom = null;
            LocalDate dateTo = null;

            if (req.getDateRange() != null) {
                if (req.getDateRange().getStart() != null) {
                    dateFrom = LocalDate.parse(req.getDateRange().getStart());
                }
                if (req.getDateRange().getEnd() != null) {
                    dateTo = LocalDate.parse(req.getDateRange().getEnd());
                }
            }
            if (dateFrom == null) dateFrom = LocalDate.now().minusDays(7);
            if (dateTo == null) dateTo = LocalDate.now();

            if (city == null || city.isEmpty()) {
                city = extractCity(req.getQuestion());
            }

            String systemPrompt;
            List<ChatResponse.SourceInfo> sources = new ArrayList<>();

            // 如果识别到城市，执行 RAG 检索
            if (city != null && !city.isEmpty()) {
                float[] questionVector = embeddingService.embed(req.getQuestion());
                List<SearchResult> results = retrieverService.search(
                        questionVector, city, dateFrom, dateTo, 10);

                if (!results.isEmpty()) {
                    List<SearchResult> deduped = deduplicateByReportId(results);
                    systemPrompt = buildSystemPrompt(deduped);
                    for (SearchResult r : deduped) {
                        sources.add(new ChatResponse.SourceInfo(r.getCity(), r.getDate(), r.getSection()));
                    }
                } else {
                    systemPrompt = defaultSystemPrompt();
                }
            } else {
                systemPrompt = defaultSystemPrompt();
            }

            String answer = llmService.chat(systemPrompt, req.getQuestion());

            ChatResponse resp = new ChatResponse();
            resp.setContent(answer);
            resp.setSources(sources);
            return resp;

        } catch (Exception e) {
            log.error("RAG 非流式处理失败: {}", e.getMessage(), e);
            ChatResponse resp = new ChatResponse();
            resp.setContent("抱歉，智能分析服务暂时不可用，请稍后重试。");
            resp.setSources(Collections.emptyList());
            return resp;
        }
    }

    /**
     * 同一报告同一段落只保留最高分 Chunk，不同段落保留
     */
    private List<SearchResult> deduplicateByReportId(List<SearchResult> results) {
        Map<String, SearchResult> best = new LinkedHashMap<>();
        for (SearchResult r : results) {
            String key = r.getReportId() + "|" + r.getSection();
            SearchResult existing = best.get(key);
            if (existing == null || r.getScore() > existing.getScore()) {
                best.put(key, r);
            }
        }
        List<SearchResult> deduped = new ArrayList<>(best.values());
        deduped.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        if (deduped.size() > 5) {
            return deduped.subList(0, 5);
        }
        return deduped;
    }

    /**
     * 构建系统 Prompt
     */
    private String buildSystemPrompt(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("当前系统日期：").append(LocalDate.now())
          .append("。请据此理解\"今天\"\"昨天\"\"明天\"等相对日期。\n");
        sb.append("你是天气数据分析助手。请根据以下检索到的历史天气分析报告片段回答用户问题。");
        sb.append("若片段信息不足以回答，请明确告知用户，不要编造数据。\n\n");
        sb.append("【检索片段】\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append(String.format("%d. [%s %s %s] %s\n",
                    i + 1, r.getCity(), r.getDate(), r.getSection(), r.getContent()));
        }
        sb.append("\n请给出简明准确的回答，并在末尾列出引用的报告日期。");
        return sb.toString();
    }

    /**
     * 从问题文本中提取城市名（简单正则匹配）
     */
    private String extractCity(String question) {
        // 常见城市名列表（从 city_list 扩展）
        String[] knownCities = {
                "北京", "上海", "广州", "深圳", "成都", "杭州", "武汉", "西安", "南京", "重庆",
                "天津", "苏州", "长沙", "郑州", "济南", "青岛", "大连", "厦门", "福州", "昆明",
                "合肥", "哈尔滨", "沈阳", "长春", "石家庄", "太原", "南昌", "贵阳", "南宁", "海口",
                "兰州", "乌鲁木齐", "呼和浩特", "拉萨", "银川", "西宁", "景德镇",
                "上饶", "九江", "赣州", "嘉兴", "温州", "宁波", "绍兴", "佛山", "东莞", "珠海",
                "洛阳", "南阳", "邯郸", "保定", "中山", "泉州", "漳州", "桂林", "柳州"
        };

        for (String c : knownCities) {
            if (question.contains(c)) {
                return c;
            }
        }
        return null;
    }
}
