package com.mathmate.dto.report;

import java.time.LocalDateTime;

public record WeeklyReportView(
        Long id,
        WeeklyFacts facts,
        WeeklyNarrative narrative,
        LocalDateTime generatedAt,
        boolean read
) {
}
