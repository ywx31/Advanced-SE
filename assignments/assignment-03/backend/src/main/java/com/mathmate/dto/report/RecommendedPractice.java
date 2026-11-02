package com.mathmate.dto.report;

import com.mathmate.dto.practice.PracticeConfig;
import com.mathmate.dto.practice.QuestionType;

import java.util.Map;

public record RecommendedPractice(
        PracticeConfig config,
        Map<QuestionType, Integer> typeQuotas,
        String reason
) {
}
