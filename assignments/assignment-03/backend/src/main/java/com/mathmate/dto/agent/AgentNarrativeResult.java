package com.mathmate.dto.agent;

import com.mathmate.dto.report.WeeklyNarrative;

public record AgentNarrativeResult(
        WeeklyNarrative narrative,
        String runStatus,
        Integer latencyMs,
        String errorMessage
) {
}
