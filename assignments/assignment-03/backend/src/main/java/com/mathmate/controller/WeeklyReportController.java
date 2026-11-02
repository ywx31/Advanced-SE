package com.mathmate.controller;

import com.mathmate.common.ApiResponse;
import com.mathmate.dto.practice.SessionDetail;
import com.mathmate.dto.report.WeeklyReportView;
import com.mathmate.security.SecurityUtils;
import com.mathmate.service.report.WeeklyReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/weekly")
public class WeeklyReportController {
    private final WeeklyReportService weeklyReportService;

    public WeeklyReportController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @GetMapping("/latest")
    public ApiResponse<WeeklyReportView> latest() {
        return ApiResponse.ok(weeklyReportService.latest(SecurityUtils.currentUserId()));
    }

    @GetMapping
    public ApiResponse<WeeklyReportView> byWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        return ApiResponse.ok(weeklyReportService.getOrGenerate(
                SecurityUtils.currentUserId(),
                weekStart
        ));
    }

    @PostMapping("/generate")
    public ApiResponse<WeeklyReportView> generate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        return ApiResponse.ok(weeklyReportService.generate(
                SecurityUtils.currentUserId(),
                weekStart
        ));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<WeeklyReportView> markRead(@PathVariable Long id) {
        return ApiResponse.ok(weeklyReportService.markRead(
                SecurityUtils.currentUserId(),
                id
        ));
    }

    @PostMapping("/{id}/recommended-practice")
    public ApiResponse<SessionDetail> createRecommendedPractice(@PathVariable Long id) {
        return ApiResponse.ok(weeklyReportService.createRecommendedPractice(
                SecurityUtils.currentUserId(),
                id
        ));
    }
}
