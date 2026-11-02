package com.mathmate.dto.stats;

import java.math.BigDecimal;

public class OverviewStats {
    private Long totalSessions;
    private Long totalQuestions;
    private Long correctQuestions;
    private BigDecimal averageAccuracy;
    private Long totalSeconds;
    private Long activeWrongQuestions;

    public Long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Long totalSessions) { this.totalSessions = totalSessions; }
    public Long getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Long totalQuestions) { this.totalQuestions = totalQuestions; }
    public Long getCorrectQuestions() { return correctQuestions; }
    public void setCorrectQuestions(Long correctQuestions) { this.correctQuestions = correctQuestions; }
    public BigDecimal getAverageAccuracy() { return averageAccuracy; }
    public void setAverageAccuracy(BigDecimal averageAccuracy) { this.averageAccuracy = averageAccuracy; }
    public Long getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(Long totalSeconds) { this.totalSeconds = totalSeconds; }
    public Long getActiveWrongQuestions() { return activeWrongQuestions; }
    public void setActiveWrongQuestions(Long activeWrongQuestions) { this.activeWrongQuestions = activeWrongQuestions; }
}
