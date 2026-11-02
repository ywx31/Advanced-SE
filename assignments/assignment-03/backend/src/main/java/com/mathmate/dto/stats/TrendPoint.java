package com.mathmate.dto.stats;

import java.math.BigDecimal;

public record TrendPoint(
        Long sessionId,
        String label,
        BigDecimal accuracy,
        int correctCount,
        int totalCount
) {
}
