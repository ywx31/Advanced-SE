package com.mathmate.dto.practice;

import java.time.LocalDateTime;
import java.util.List;

public record WorksheetView(
        String title,
        PracticeConfig config,
        LocalDateTime generatedAt,
        int totalCount,
        List<WorksheetQuestion> questions
) {
}
