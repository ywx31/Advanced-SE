package com.mathmate.dto.stats;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ActivityDay(
        LocalDate date,
        long sessionCount,
        long questionCount,
        BigDecimal accuracy
) {
}
