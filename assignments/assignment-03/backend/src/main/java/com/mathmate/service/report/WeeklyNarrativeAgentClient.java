package com.mathmate.service.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathmate.dto.agent.AgentNarrativeResult;
import com.mathmate.dto.agent.WeeklyNarrativePayload;
import com.mathmate.dto.report.WeeklyFacts;
import com.mathmate.dto.report.WeeklyNarrative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class WeeklyNarrativeAgentClient {
    private static final Pattern UNSAFE_CONTENT = Pattern.compile(
            "(https?://|www\\.|\\b\\d{11}\\b|忽略(之前|以上)|系统提示|system prompt|api[_ -]?key)",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper;
    private final WeeklyNarrativeFactory fallbackFactory;
    private final HttpClient httpClient;
    private final String weeklyUrl;
    private final int timeoutMs;
    private final boolean enabled;

    public WeeklyNarrativeAgentClient(
            ObjectMapper objectMapper,
            WeeklyNarrativeFactory fallbackFactory,
            @Value("${app.agent.weekly-url}") String weeklyUrl,
            @Value("${app.agent.timeout-ms:8000}") int timeoutMs,
            @Value("${app.agent.weekly-enabled:true}") boolean enabled
    ) {
        this.objectMapper = objectMapper;
        this.fallbackFactory = fallbackFactory;
        this.weeklyUrl = weeklyUrl;
        this.timeoutMs = Math.max(500, timeoutMs);
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.timeoutMs))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public AgentNarrativeResult generate(WeeklyFacts facts) {
        if (!enabled) {
            return new AgentNarrativeResult(
                    fallbackFactory.create(facts),
                    "TEMPLATE",
                    0,
                    "Agent 未启用"
            );
        }

        long started = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(weeklyUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(facts)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            int latency = elapsedMs(started);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallback(facts, latency, "Agent HTTP " + response.statusCode(), "ERROR");
            }

            WeeklyNarrativePayload payload = objectMapper.readValue(
                    response.body(),
                    WeeklyNarrativePayload.class
            );
            String validationError = validate(payload);
            if (validationError != null) {
                return fallback(facts, latency, validationError, "REJECTED");
            }

            WeeklyNarrative narrative = new WeeklyNarrative(
                    payload.summary().trim(),
                    cleanList(payload.highlights()),
                    cleanList(payload.suggestions()),
                    payload.source(),
                    payload.model(),
                    payload.moderationStatus()
            );
            String status = "AGENT".equals(payload.source()) ? "SUCCESS" : "TEMPLATE";
            return new AgentNarrativeResult(
                    narrative,
                    status,
                    latency,
                    "AGENT".equals(payload.source()) ? null : safeMessage(payload.errorMessage())
            );
        } catch (Exception exception) {
            return fallback(
                    facts,
                    elapsedMs(started),
                    exception.getClass().getSimpleName() + ": " + safeMessage(exception.getMessage()),
                    "ERROR"
            );
        }
    }

    private AgentNarrativeResult fallback(
            WeeklyFacts facts,
            int latency,
            String error,
            String moderationStatus
    ) {
        WeeklyNarrative template = fallbackFactory.create(facts);
        WeeklyNarrative narrative = new WeeklyNarrative(
                template.summary(),
                template.highlights(),
                template.suggestions(),
                "TEMPLATE",
                null,
                moderationStatus.equals("REJECTED") ? "REJECTED" : "PASSED"
        );
        return new AgentNarrativeResult(narrative, "TEMPLATE", latency, safeMessage(error));
    }

    private String validate(WeeklyNarrativePayload payload) {
        if (payload == null) {
            return "Agent 返回为空";
        }
        if (payload.summary() == null || payload.summary().isBlank() || payload.summary().length() > 800) {
            return "Agent 总结长度不合法";
        }
        if (payload.highlights() == null || payload.highlights().size() > 5
                || payload.suggestions() == null || payload.suggestions().size() > 5) {
            return "Agent 列表数量不合法";
        }
        if (!List.of("AGENT", "TEMPLATE").contains(payload.source())) {
            return "Agent 来源标识不合法";
        }
        if (!"PASSED".equals(payload.moderationStatus())) {
            return "Agent 内容审核未通过";
        }
        if ("AGENT".equals(payload.source())
                && (payload.model() == null || payload.model().isBlank())) {
            return "Agent 缺少模型标识";
        }
        List<String> lines = new java.util.ArrayList<>();
        lines.add(payload.summary());
        lines.addAll(payload.highlights());
        lines.addAll(payload.suggestions());
        for (String line : lines) {
            if (line == null || line.isBlank() || line.length() > 300
                    || UNSAFE_CONTENT.matcher(line).find()) {
                return "Agent 内容包含不合法文本";
            }
        }
        return null;
    }

    private List<String> cleanList(List<String> values) {
        return values.stream().map(String::trim).toList();
    }

    private int elapsedMs(long started) {
        return (int) Math.min(Integer.MAX_VALUE, (System.nanoTime() - started) / 1_000_000);
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String safe = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return safe.length() > 450 ? safe.substring(0, 450) : safe;
    }
}
