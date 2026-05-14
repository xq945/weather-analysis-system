package com.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.WeatherData;
import com.weather.entity.WeatherForecast;
import com.weather.entity.WeatherReport;
import com.weather.mapper.WeatherDataMapper;
import com.weather.mapper.WeatherForecastMapper;
import com.weather.mapper.WeatherReportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final WeatherDataMapper weatherDataMapper;
    private final WeatherForecastMapper weatherForecastMapper;
    private final WeatherReportMapper weatherReportMapper;
    private final EmbeddingService embeddingService;
    private final RetrieverService retrieverService;

    public ReportService(WeatherDataMapper weatherDataMapper,
                         WeatherForecastMapper weatherForecastMapper,
                         WeatherReportMapper weatherReportMapper,
                         EmbeddingService embeddingService,
                         RetrieverService retrieverService) {
        this.weatherDataMapper = weatherDataMapper;
        this.weatherForecastMapper = weatherForecastMapper;
        this.weatherReportMapper = weatherReportMapper;
        this.embeddingService = embeddingService;
        this.retrieverService = retrieverService;
    }

    /**
     * 生成单份报告并同步到 Qdrant
     */
    public WeatherReport generateReport(String city, LocalDate date, Integer reportType) {
        log.info("开始生成报告: city={}, date={}, type={}", city, date, reportType);

        // 1. 查询天气数据
        List<WeatherData> dataList = weatherDataMapper.selectList(
                new LambdaQueryWrapper<WeatherData>()
                        .eq(WeatherData::getCity, city)
                        .between(WeatherData::getObsTime,
                                date.atStartOfDay(),
                                date.plusDays(1).atStartOfDay())
                        .orderByAsc(WeatherData::getObsTime)
        );

        // 2. 查询预报数据
        WeatherForecast forecast = weatherForecastMapper.selectOne(
                new LambdaQueryWrapper<WeatherForecast>()
                        .eq(WeatherForecast::getCity, city)
                        .eq(WeatherForecast::getForecastDate, date)
        );

        // 3. 数据不足时仍生成基础报告
        // 4. 生成 Markdown 报告
        String content = buildReportContent(city, date, dataList, forecast);

        // 5. 幂等处理：先删旧报告
        deleteByCityAndDate(city, date);

        // 6. 写入 MySQL
        String reportId = "rpt_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + city + "_001";
        WeatherReport report = new WeatherReport();
        report.setReportId(reportId);
        report.setCity(city);
        report.setReportDate(date);
        report.setReportType(reportType);
        report.setTitle(city + "天气" + (reportType == 1 ? "日报" : reportType == 2 ? "周报" : "深度分析") + "(" + date + ")");
        report.setContent(content);
        report.setChunkCount(0);
        report.setQdrantSynced(0);
        weatherReportMapper.insert(report);

        // 7. 向量化并写入 Qdrant
        try {
            List<ChunkInfo> chunks = splitIntoChunks(report);
            List<String> chunkTexts = chunks.stream().map(ChunkInfo::content).toList();
            List<float[]> vectors = embeddingService.embedBatch(chunkTexts);
            List<String> sections = chunks.stream().map(ChunkInfo::section).toList();

            float tempMax = forecast != null && forecast.getTempMax() != null ? forecast.getTempMax() : 0f;
            float tempMin = forecast != null && forecast.getTempMin() != null ? forecast.getTempMin() : 0f;
            String weatherText = forecast != null && forecast.getWeatherTextDay() != null
                    ? forecast.getWeatherTextDay() : "晴";

            List<Float> tempMaxes = new ArrayList<>();
            List<Float> tempMins = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                tempMaxes.add(tempMax);
                tempMins.add(tempMin);
            }

            retrieverService.deleteByReportId(reportId);
            retrieverService.upsertChunks(reportId, chunkTexts, vectors, city,
                    date.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toString(),
                    reportType, sections, tempMaxes, tempMins, weatherText);

            report.setChunkCount(chunks.size());
            report.setQdrantSynced(1);
            weatherReportMapper.updateById(report);
            log.info("报告生成完成: reportId={}, chunks={}", reportId, chunks.size());
        } catch (Exception e) {
            log.error("向量同步失败: reportId={}, error={}", reportId, e.getMessage());
            // 报告已保存，qdrant_synced 保持 0，由对账任务修复
        }

        return report;
    }

    /**
     * 按城市和日期删除
     */
    public void deleteByCityAndDate(String city, LocalDate date) {
        LambdaQueryWrapper<WeatherReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeatherReport::getCity, city)
               .eq(WeatherReport::getReportDate, date);
        List<WeatherReport> existing = weatherReportMapper.selectList(wrapper);
        for (WeatherReport r : existing) {
            retrieverService.deleteByReportId(r.getReportId());
            weatherReportMapper.deleteById(r.getId());
        }
    }

    /**
     * 按 reportId 删除
     */
    public void deleteByReportId(String reportId) {
        LambdaQueryWrapper<WeatherReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeatherReport::getReportId, reportId);
        WeatherReport report = weatherReportMapper.selectOne(wrapper);
        if (report != null) {
            retrieverService.deleteByReportId(reportId);
            weatherReportMapper.deleteById(report.getId());
        }
    }

    /**
     * 重新同步向量库
     */
    public void resyncToQdrant(Long id) {
        WeatherReport report = weatherReportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("报告不存在: " + id);
        }
        List<ChunkInfo> chunks = splitIntoChunks(report);
        List<String> chunkTexts = chunks.stream().map(ChunkInfo::content).toList();
        List<float[]> vectors = embeddingService.embedBatch(chunkTexts);
        List<String> sections = chunks.stream().map(ChunkInfo::section).toList();

        retrieverService.deleteByReportId(report.getReportId());
        retrieverService.upsertChunks(report.getReportId(), chunkTexts, vectors,
                report.getCity(),
                report.getReportDate().atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toString(),
                report.getReportType(),
                sections,
                Collections.nCopies(chunks.size(), 0f),
                Collections.nCopies(chunks.size(), 0f),
                "晴");

        report.setChunkCount(chunks.size());
        report.setQdrantSynced(1);
        weatherReportMapper.updateById(report);
    }

    /**
     * 查询报告列表
     */
    public List<WeatherReport> listReports(String city, LocalDate date) {
        LambdaQueryWrapper<WeatherReport> wrapper = new LambdaQueryWrapper<>();
        if (city != null && !city.isEmpty()) {
            wrapper.eq(WeatherReport::getCity, city);
        }
        if (date != null) {
            wrapper.eq(WeatherReport::getReportDate, date);
        }
        wrapper.orderByDesc(WeatherReport::getCreatedAt);
        return weatherReportMapper.selectList(wrapper);
    }

    /**
     * 根据 reportId 查询
     */
    public WeatherReport getByReportId(String reportId) {
        LambdaQueryWrapper<WeatherReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeatherReport::getReportId, reportId);
        return weatherReportMapper.selectOne(wrapper);
    }

    /**
     * 未同步报告数
     */
    public int countUnsyncedReports() {
        LambdaQueryWrapper<WeatherReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeatherReport::getQdrantSynced, 0);
        long count = weatherReportMapper.selectCount(wrapper);
        return (int) count;
    }

    /**
     * 生成 Markdown 报告文本
     */
    private String buildReportContent(String city, LocalDate date,
                                       List<WeatherData> dataList, WeatherForecast forecast) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(city).append(" 天气分析报告 (").append(date).append(")\n\n");

        // 概况
        sb.append("## 一、概况\n");
        if (!dataList.isEmpty()) {
            double avgTemp = dataList.stream().mapToDouble(WeatherData::getTemp).average().orElse(0);
            double maxTemp = dataList.stream().mapToDouble(WeatherData::getTemp).max().orElse(0);
            double minTemp = dataList.stream().mapToDouble(WeatherData::getTemp).min().orElse(0);
            double avgHumidity = dataList.stream().mapToDouble(WeatherData::getHumidity).average().orElse(0);
            String mainWeather = dataList.get(dataList.size() - 1).getWeatherText();

            sb.append(String.format("今日平均温度 %.1f℃，最高 %.1f℃，最低 %.1f℃，主导天气为 %s，平均湿度 %.0f%%。\n",
                    avgTemp, maxTemp, minTemp, mainWeather, avgHumidity));
        } else if (forecast != null) {
            sb.append(String.format("预报温度 %s℃ ~ %s℃，白天天气 %s，夜间天气 %s。\n",
                    forecast.getTempMin(), forecast.getTempMax(),
                    forecast.getWeatherTextDay(), forecast.getWeatherTextNight()));
        } else {
            sb.append("暂无数据。\n");
        }

        // 趋势分析
        sb.append("\n## 二、趋势分析\n");
        if (dataList.size() >= 2) {
            WeatherData first = dataList.get(0);
            WeatherData last = dataList.get(dataList.size() - 1);
            double tempChange = last.getTemp() - first.getTemp();
            String dir = tempChange > 0 ? "上升" : "下降";
            sb.append(String.format("与昨日同期对比，温度%s %.1f℃，", dir, Math.abs(tempChange)));

            // 最高最低趋势
            double firstHigh = dataList.get(0).getTemp();
            double lastHigh = dataList.get(dataList.size() - 1).getTemp();
            sb.append(String.format("日内温度范围 %.1f℃ ~ %.1f℃。", Math.min(firstHigh, lastHigh),
                    Math.max(firstHigh, lastHigh)));

            // 风力湿度
            double avgWind = dataList.stream().mapToDouble(WeatherData::getWindSpeed).average().orElse(0);
            sb.append(String.format("平均风力 %.1f 级。", avgWind));
        } else {
            sb.append("数据不足，无法进行趋势分析。\n");
        }

        // 异常提示
        sb.append("\n## 三、异常提示\n");
        boolean hasAbnormal = false;
        if (forecast != null && forecast.getTempMax() != null && forecast.getTempMin() != null) {
            if (forecast.getTempMax() - forecast.getTempMin() > 15) {
                sb.append("注意：昼夜温差超过 15℃，请注意增减衣物。");
                hasAbnormal = true;
            }
        }
        if (!dataList.isEmpty()) {
            double maxWind = dataList.stream().mapToDouble(WeatherData::getWindSpeed).max().orElse(0);
            if (maxWind > 10) {
                sb.append("注意：风力较大，出行请注意安全。");
                hasAbnormal = true;
            }
        }
        if (!hasAbnormal) {
            sb.append("当前无明显异常天气。");
        }

        // 出行建议
        sb.append("\n\n## 四、出行建议\n");
        if (forecast != null) {
            String dayWeather = forecast.getWeatherTextDay();
            if (dayWeather != null) {
                if (dayWeather.contains("雨") || dayWeather.contains("雪")) {
                    sb.append("建议携带雨具。");
                } else if (dayWeather.contains("晴") || dayWeather.contains("多云")) {
                    sb.append("今日天气良好，适宜户外活动。");
                } else if (dayWeather.contains("阴")) {
                    sb.append("今日天气阴沉，户外活动体验一般。");
                } else {
                    sb.append("请根据实际天气情况安排出行。");
                }

                if (forecast.getTempMax() != null) {
                    if (forecast.getTempMax() > 35) {
                        sb.append("高温天气，注意防暑降温。");
                    } else if (forecast.getTempMax() < 5) {
                        sb.append("低温天气，注意保暖防寒。");
                    }
                }
            }
        } else {
            sb.append("暂无预报数据，建议关注实时天气。");
        }

        return sb.toString();
    }

    /**
     * 按二级标题切分报告
     */
    public List<ChunkInfo> splitIntoChunks(WeatherReport report) {
        List<ChunkInfo> chunks = new ArrayList<>();
        String content = report.getContent();
        String prefix = "【" + report.getCity() + " " + report.getReportDate() + " 天气报告";

        String[] sections = {"## 一、概况", "## 二、趋势分析", "## 三、异常提示", "## 四、出行建议"};
        String[] sectionNames = {"概况", "趋势分析", "异常提示", "出行建议"};

        for (int i = 0; i < sections.length; i++) {
            int start = content.indexOf(sections[i]);
            if (start == -1) continue;

            int end = (i + 1 < sections.length) ? content.indexOf(sections[i + 1]) : content.length();
            if (end == -1) end = content.length();

            String chunkContent = prefix + " - " + sectionNames[i] + "】"
                    + content.substring(start, end).trim();

            chunks.add(new ChunkInfo(sectionNames[i], chunkContent));
        }

        return chunks;
    }

    public record ChunkInfo(String section, String content) {}
}
