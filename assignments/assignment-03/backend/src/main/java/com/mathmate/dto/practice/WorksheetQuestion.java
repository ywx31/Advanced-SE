package com.mathmate.dto.practice;

public record WorksheetQuestion(
        int number,
        String expression,
        QuestionType type,
        int answer
) {
}
