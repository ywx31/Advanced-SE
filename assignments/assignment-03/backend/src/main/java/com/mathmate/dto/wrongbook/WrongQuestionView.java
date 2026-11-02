package com.mathmate.dto.wrongbook;

import com.mathmate.dto.practice.QuestionType;

import java.time.LocalDateTime;

public record WrongQuestionView(
        Long id,
        String expression,
        QuestionType questionType,
        int correctAnswer,
        Integer lastWrongAnswer,
        int wrongCount,
        int correctStreak,
        boolean mastered,
        LocalDateTime lastPracticedAt
) {
}
