package com.weather.controller;

import com.weather.dto.Result;
import com.weather.entity.WeatherReport;
import com.weather.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

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

    @GetMapping("/report/list")
    public Result<List<WeatherReport>> list(@RequestParam(required = false) String city,
                                             @RequestParam(required = false) String date) {
        LocalDate localDate = null;
        if (date != null && !date.isEmpty()) {
            localDate = LocalDate.parse(date);
        }
        return Result.ok(reportService.listReports(city, localDate));
    }

    @GetMapping("/report/{reportId}")
    public Result<WeatherReport> detail(@PathVariable String reportId) {
        WeatherReport report = reportService.getByReportId(reportId);
        if (report == null) {
            return Result.error(404, "报告不存在");
        }
        return Result.ok(report);
    }

    @DeleteMapping("/admin/report/{reportId}")
    public Result<?> delete(@PathVariable String reportId) {
        reportService.deleteByReportId(reportId);
        return Result.ok();
    }

    @PostMapping("/admin/report/sync/{id}")
    public Result<?> resync(@PathVariable Long id) {
        reportService.resyncToQdrant(id);
        return Result.ok();
    }
}
