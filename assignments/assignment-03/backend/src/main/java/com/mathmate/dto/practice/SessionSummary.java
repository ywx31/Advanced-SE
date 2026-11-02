package com.mathmate.dto.practice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SessionSummary(
        Long id,
        String title,
        int totalCount,
        int correctCount,
        BigDecimal accuracy,
        int durationSeconds,
        String status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        List<QuestionType> types
) {
}
