package com.mathmate.dto.report;

public record WeeklyMistake(
        String expression,
        String type,
        String typeLabel,
        int errorCount,
        int correctAnswer
) {
}
