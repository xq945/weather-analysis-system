package com.weather.controller;

import com.weather.dto.Result;
import com.weather.entity.WeatherReport;
import com.weather.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 分析报告接口：生成、查询、删除、向量同步
 */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 生成天气分析报告
     *
     * 自动查询天气数据，生成 Markdown 报告，并向量化同步到 Qdrant。
     *
     * @param body 包含 city（城市）、date（日期）、reportType（类型 1/2/3）
     * @return 生成的报告实体
     */
    @PostMapping("/admin/report/generate")
    public Result<?> generate(@RequestBody Map<String, Object> body) {
        String city = (String) body.get("city");
        String date = (String) body.get("date");
        Integer reportType = body.get("reportType") != null
                ? ((Number) body.get("reportType")).intValue() : 1;

        if (city == null || city.isEmpty()) {
            return Result.error(400, "请指定城市");
        }
        if (date == null || date.isEmpty()) {
            return Result.error(400, "请指定日期");
        }

        WeatherReport report = reportService.generateReport(city, LocalDate.parse(date), reportType);
        return Result.ok(report);
    }

    /**
     * 查询报告列表
     *
     * @param city 可选，按城市过滤
     * @param date 可选，按日期过滤（yyyy-MM-dd）
     * @return 报告列表，按创建时间倒序
     */
    @GetMapping("/report/list")
    public Result<List<WeatherReport>> list(@RequestParam(required = false) String city,
                                             @RequestParam(required = false) String date) {
        LocalDate localDate = null;
        if (date != null && !date.isEmpty()) {
            localDate = LocalDate.parse(date);
        }
        return Result.ok(reportService.listReports(city, localDate));
    }

    /** 获取报告详情 */
    @GetMapping("/report/{reportId}")
    public Result<WeatherReport> detail(@PathVariable String reportId) {
        WeatherReport report = reportService.getByReportId(reportId);
        if (report == null) {
            return Result.error(404, "报告不存在");
        }
        return Result.ok(report);
    }

    /** 删除报告（同时删除 Qdrant 中的向量） */
    @DeleteMapping("/admin/report/{reportId}")
    public Result<?> delete(@PathVariable String reportId) {
        reportService.deleteByReportId(reportId);
        return Result.ok();
    }

    /** 重新同步指定报告到向量库 */
    @PostMapping("/admin/report/sync/{id}")
    public Result<?> resync(@PathVariable Long id) {
        reportService.resyncToQdrant(id);
        return Result.ok();
    }
}
