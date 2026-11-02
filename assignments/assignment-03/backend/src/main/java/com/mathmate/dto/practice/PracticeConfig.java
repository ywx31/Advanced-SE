package com.mathmate.dto.practice;

import java.util.List;

public record PracticeConfig(
        String title,
        int count,
        List<QuestionType> types,
        int operandCount,
        boolean allowParentheses,
        int maxNumber,
        int maxFactor
) {
}
