package com.mathmate.dto.stats;

import java.math.BigDecimal;

public record DailyPoint(
        String date,
        long sessionCount,
        long questionCount,
        BigDecimal accuracy
) {
}
