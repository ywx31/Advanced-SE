package com.mathmate.dto.practice;

public record QuestionView(
        Long id,
        int number,
        String expression,
        QuestionType type,
        Integer userAnswer,
        Integer correctAnswer,
        Boolean correct
) {
}
