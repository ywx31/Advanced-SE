package com.mathmate.controller;

import com.mathmate.common.ApiResponse;
import com.mathmate.dto.stats.DashboardResponse;
import com.mathmate.security.SecurityUtils;
import com.mathmate.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.ok(statsService.dashboard(SecurityUtils.currentUserId()));
    }
}
