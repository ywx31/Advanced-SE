package com.mathmate.dto.stats;

import com.mathmate.dto.practice.SessionSummary;

import java.util.List;

public record DashboardResponse(
        OverviewStats overview,
        List<TrendPoint> trend,
        List<TypeAccuracy> typeAccuracy,
        List<DailyPoint> daily,
        List<ActivityDay> activity,
        List<SessionSummary> recentSessions,
        int streakDays
) {
}
