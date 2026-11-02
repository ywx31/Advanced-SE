package com.mathmate.dto.agent;

import java.util.List;

public record WeeklyNarrativePayload(
        String summary,
        List<String> highlights,
        List<String> suggestions,
        String source,
        String model,
        String moderationStatus,
        String errorMessage
) {
}
