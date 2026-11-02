package com.mathmate.dto.stats;

import java.math.BigDecimal;

public record TypeAccuracy(
        String type,
        String label,
        long total,
        long correct,
        BigDecimal accuracy
) {
}
