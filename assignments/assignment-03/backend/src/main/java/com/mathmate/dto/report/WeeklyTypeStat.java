package com.mathmate.dto.report;

import java.math.BigDecimal;

public record WeeklyTypeStat(
        String type,
        String label,
        long total,
        long correct,
        BigDecimal accuracy
) {
}
