package com.mathmate.dto.practice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SessionDetail(
        Long id,
        String title,
        PracticeConfig config,
        int totalCount,
        int correctCount,
        BigDecimal accuracy,
        int durationSeconds,
        String status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        List<QuestionView> questions
) {
}
