package com.mathmate.dto.report;

import java.math.BigDecimal;

public record WeeklyOverview(
        long sessionCount,
        long questionCount,
        long correctCount,
        BigDecimal accuracy,
        BigDecimal previousAccuracy,
        BigDecimal accuracyDelta,
        long studyMinutes
) {
}
