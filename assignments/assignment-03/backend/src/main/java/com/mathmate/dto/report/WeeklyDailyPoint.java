package com.mathmate.dto.report;

import java.math.BigDecimal;

public record WeeklyDailyPoint(
        String date,
        long questionCount,
        BigDecimal accuracy
) {
}
