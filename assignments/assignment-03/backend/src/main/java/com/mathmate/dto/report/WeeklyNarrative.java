package com.mathmate.dto.report;

import java.util.List;

public record WeeklyNarrative(
        String summary,
        List<String> highlights,
        List<String> suggestions,
        String source,
        String model,
        String moderationStatus
) {
}
