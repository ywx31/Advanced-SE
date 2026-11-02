package com.mathmate.service.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathmate.dto.agent.AgentNarrativeResult;
import com.mathmate.dto.practice.GeneratedQuestion;
import com.mathmate.dto.practice.PracticeConfig;
import com.mathmate.dto.practice.PracticeConfigRequest;
import com.mathmate.dto.practice.QuestionType;
import com.mathmate.dto.practice.SessionDetail;
import com.mathmate.dto.report.RecommendedPractice;
import com.mathmate.dto.report.WeeklyFacts;
import com.mathmate.dto.report.WeeklyNarrative;
import com.mathmate.dto.report.WeeklyReportView;
import com.mathmate.entity.AgentRun;
import com.mathmate.entity.WeeklyReport;
import com.mathmate.exception.BusinessException;
import com.mathmate.mapper.AgentRunMapper;
import com.mathmate.mapper.WeeklyReportMapper;
import com.mathmate.service.PracticeService;
import com.mathmate.service.QuestionGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WeeklyReportService {
    private static final Logger log = LoggerFactory.getLogger(WeeklyReportService.class);

    private final WeeklyReportMapper reportMapper;
    private final AgentRunMapper agentRunMapper;
    private final WeeklyMetricsService metricsService;
    private final WeeklyNarrativeAgentClient agentClient;
    private final QuestionGeneratorService generatorService;
    private final PracticeService practiceService;
    private final ObjectMapper objectMapper;

    public WeeklyReportService(
            WeeklyReportMapper reportMapper,
            AgentRunMapper agentRunMapper,
            WeeklyMetricsService metricsService,
            WeeklyNarrativeAgentClient agentClient,
            QuestionGeneratorService generatorService,
            PracticeService practiceService,
            ObjectMapper objectMapper
    ) {
        this.reportMapper = reportMapper;
        this.agentRunMapper = agentRunMapper;
        this.metricsService = metricsService;
        this.agentClient = agentClient;
        this.generatorService = generatorService;
        this.practiceService = practiceService;
        this.objectMapper = objectMapper;
    }

    public WeeklyReportView latest(Long userId) {
        LocalDate currentWeek = metricsService.currentWeekStart();
        WeeklyReport latest = reportMapper.findLatest(userId);
        if (latest == null || latest.getWeekStart().isBefore(currentWeek)) {
            return generate(userId, currentWeek);
        }
        return toView(latest);
    }

    public WeeklyReportView getOrGenerate(Long userId, LocalDate weekStart) {
        LocalDate normalized = metricsService.normalizeWeekStart(weekStart);
        WeeklyReport report = reportMapper.findByUserAndWeek(userId, normalized);
        return report == null ? generate(userId, normalized) : toView(report);
    }

    public WeeklyReportView generate(Long userId, LocalDate requestedWeekStart) {
        LocalDate weekStart = metricsService.normalizeWeekStart(requestedWeekStart);
        LocalDate currentWeek = metricsService.currentWeekStart();
        if (weekStart.isAfter(currentWeek)) {
            throw new BusinessException("不能生成未来周的学习周报");
        }

        WeeklyFacts facts = metricsService.calculate(userId, weekStart);
        AgentNarrativeResult agentResult = agentClient.generate(facts);
        WeeklyNarrative narrative = agentResult.narrative();

        WeeklyReport report = new WeeklyReport();
        report.setUserId(userId);
        report.setWeekStart(facts.weekStart());
        report.setWeekEnd(facts.weekEnd());
        report.setMetricsJson(toJson(facts, "周报指标序列化失败"));
        report.setNarrativeJson(toJson(narrative, "周报总结序列化失败"));
        report.setNarrativeSource(narrative.source());
        report.setAgentModel(narrative.model());
        report.setRecommendationJson(toJson(facts.recommendation(), "推荐练习序列化失败"));
        reportMapper.upsert(report);

        AgentRun run = new AgentRun();
        run.setUserId(userId);
        run.setReportId(report.getId());
        run.setProvider("AGENT".equals(narrative.source()) ? "deepseek" : "template");
        run.setModel(narrative.model());
        run.setStatus(agentResult.runStatus());
        run.setLatencyMs(agentResult.latencyMs());
        run.setModerationStatus(narrative.moderationStatus());
        run.setErrorMessage(agentResult.errorMessage());
        agentRunMapper.insert(run);

        return toView(reportMapper.findByIdAndUser(report.getId(), userId));
    }

    public WeeklyReportView markRead(Long userId, Long reportId) {
        WeeklyReport report = reportMapper.findByIdAndUser(reportId, userId);
        if (report == null) {
            throw new BusinessException("周报不存在", HttpStatus.NOT_FOUND);
        }
        reportMapper.markRead(reportId, userId);
        return toView(reportMapper.findByIdAndUser(reportId, userId));
    }

    public SessionDetail createRecommendedPractice(Long userId, Long reportId) {
        WeeklyReport report = reportMapper.findByIdAndUser(reportId, userId);
        if (report == null) {
            throw new BusinessException("周报不存在", HttpStatus.NOT_FOUND);
        }
        RecommendedPractice recommendation;
        try {
            recommendation = objectMapper.readValue(report.getRecommendationJson(), RecommendedPractice.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("推荐配置不可用，请重新生成周报");
        }
        PracticeConfig config = recommendation.config();
        if (config == null || config.types() == null || config.types().isEmpty()) {
            throw new BusinessException("推荐练习配置不完整，请重新生成周报");
        }

        List<GeneratedQuestion> generated = new ArrayList<>();
        Set<String> excluded = new HashSet<>();
        Map<QuestionType, Integer> quotas = recommendation.typeQuotas();
        if (quotas == null || quotas.isEmpty()) {
            quotas = new LinkedHashMap<>();
            int base = config.count() / config.types().size();
            int remainder = config.count() % config.types().size();
            for (int index = 0; index < config.types().size(); index++) {
                quotas.put(config.types().get(index), base + (index < remainder ? 1 : 0));
            }
        }
        for (Map.Entry<QuestionType, Integer> entry : quotas.entrySet()) {
            int count = Math.min(50, Math.max(1, entry.getValue()));
            PracticeConfig part = new PracticeConfig(
                    config.title(),
                    count,
                    List.of(entry.getKey()),
                    config.operandCount(),
                    config.allowParentheses() && entry.getKey() == QuestionType.MIXED_OPERATIONS,
                    config.maxNumber(),
                    config.maxFactor()
            );
            List<GeneratedQuestion> batch = generatorService.generate(part, excluded);
            generated.addAll(batch);
            batch.forEach(item -> excluded.add(item.canonicalKey()));
        }
        if (generated.isEmpty()) {
            throw new BusinessException("推荐练习生成失败，请稍后重试");
        }
        return practiceService.createCustomSession(
                userId,
                config.title(),
                config.types(),
                config.operandCount(),
                generated
        );
    }

    @Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Shanghai")
    public void generatePreviousWeekReports() {
        LocalDate previousWeek = metricsService.currentWeekStart().minusWeeks(1);
        var start = previousWeek.atStartOfDay();
        var end = previousWeek.plusWeeks(1).atStartOfDay();
        for (Long userId : reportMapper.findActiveUserIds(start, end)) {
            try {
                generate(userId, previousWeek);
            } catch (Exception exception) {
                log.error("自动生成用户 {} 的周报失败", userId, exception);
            }
        }
    }

    private WeeklyReportView toView(WeeklyReport report) {
        if (report == null) {
            throw new BusinessException("周报不存在", HttpStatus.NOT_FOUND);
        }
        try {
            WeeklyFacts facts = objectMapper.readValue(report.getMetricsJson(), WeeklyFacts.class);
            WeeklyNarrative narrative = objectMapper.readValue(
                    report.getNarrativeJson(),
                    WeeklyNarrative.class
            );
            return new WeeklyReportView(
                    report.getId(),
                    facts,
                    narrative,
                    report.getGeneratedAt(),
                    report.getReadAt() != null
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessException("周报数据损坏，请重新生成");
        }
    }

    private String toJson(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(errorMessage);
        }
    }
}
