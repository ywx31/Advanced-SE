package com.mathmate.dto.report;

import java.time.LocalDate;
import java.util.List;

public record WeeklyFacts(
        LocalDate weekStart,
        LocalDate weekEnd,
        WeeklyOverview overview,
        List<WeeklyTypeStat> weakTypes,
        List<WeeklyMistake> frequentMistakes,
        List<WeeklyDailyPoint> daily,
        int streakDays,
        RecommendedPractice recommendation
) {
}
