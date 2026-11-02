package com.mathmate.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WeeklyReport {
    private Long id;
    private Long userId;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private String metricsJson;
    private String narrativeJson;
    private String narrativeSource;
    private String agentModel;
    private String recommendationJson;
    private LocalDateTime generatedAt;
    private LocalDateTime readAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }
    public LocalDate getWeekEnd() { return weekEnd; }
    public void setWeekEnd(LocalDate weekEnd) { this.weekEnd = weekEnd; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public String getNarrativeJson() { return narrativeJson; }
    public void setNarrativeJson(String narrativeJson) { this.narrativeJson = narrativeJson; }
    public String getNarrativeSource() { return narrativeSource; }
    public void setNarrativeSource(String narrativeSource) { this.narrativeSource = narrativeSource; }
    public String getAgentModel() { return agentModel; }
    public void setAgentModel(String agentModel) { this.agentModel = agentModel; }
    public String getRecommendationJson() { return recommendationJson; }
    public void setRecommendationJson(String recommendationJson) { this.recommendationJson = recommendationJson; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
