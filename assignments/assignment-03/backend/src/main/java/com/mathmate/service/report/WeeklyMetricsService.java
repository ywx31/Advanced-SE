package com.mathmate.service.report;

import com.mathmate.dto.practice.PracticeConfig;
import com.mathmate.dto.practice.QuestionType;
import com.mathmate.dto.report.RecommendedPractice;
import com.mathmate.dto.report.WeeklyDailyPoint;
import com.mathmate.dto.report.WeeklyFacts;
import com.mathmate.dto.report.WeeklyMistake;
import com.mathmate.dto.report.WeeklyOverview;
import com.mathmate.dto.report.WeeklyTypeStat;
import com.mathmate.mapper.StatsMapper;
import com.mathmate.mapper.WeeklyReportMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class WeeklyMetricsService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");
    private static final int MIN_TYPE_SAMPLE = 3;

    private final WeeklyReportMapper reportMapper;
    private final StatsMapper statsMapper;

    public WeeklyMetricsService(WeeklyReportMapper reportMapper, StatsMapper statsMapper) {
        this.reportMapper = reportMapper;
        this.statsMapper = statsMapper;
    }

    public LocalDate currentWeekStart() {
        return LocalDate.now(ZONE)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public LocalDate normalizeWeekStart(LocalDate requested) {
        LocalDate date = requested == null ? currentWeekStart() : requested;
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public WeeklyFacts calculate(Long userId, LocalDate requestedWeekStart) {
        LocalDate weekStart = normalizeWeekStart(requestedWeekStart);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusWeeks(1).atStartOfDay();
        LocalDateTime previousStart = weekStart.minusWeeks(1).atStartOfDay();
        LocalDateTime previousEnd = start;

        Map<String, Object> current = nullSafe(reportMapper.weeklySummary(userId, start, end));
        Map<String, Object> previous = nullSafe(reportMapper.weeklySummary(userId, previousStart, previousEnd));

        BigDecimal accuracy = decimal(current, "accuracy");
        BigDecimal previousAccuracy = decimal(previous, "accuracy");
        WeeklyOverview overview = new WeeklyOverview(
                number(current, "sessionCount").longValue(),
                number(current, "questionCount").longValue(),
                number(current, "correctCount").longValue(),
                accuracy,
                previousAccuracy,
                accuracy.subtract(previousAccuracy).setScale(2, RoundingMode.HALF_UP),
                Math.round(number(current, "studySeconds").doubleValue() / 60.0)
        );

        List<WeeklyTypeStat> allTypes = reportMapper.typeStats(userId, start, end).stream()
                .map(row -> new WeeklyTypeStat(
                        string(row, "type"),
                        typeLabel(string(row, "type")),
                        number(row, "total").longValue(),
                        number(row, "correct").longValue(),
                        decimal(row, "accuracy")
                ))
                .toList();
        List<WeeklyTypeStat> weakTypes = allTypes.stream()
                .filter(item -> item.total() >= MIN_TYPE_SAMPLE)
                .limit(3)
                .toList();

        List<WeeklyMistake> mistakes = reportMapper.mistakeStats(userId, start, end).stream()
                .map(row -> new WeeklyMistake(
                        string(row, "expression"),
                        string(row, "type"),
                        typeLabel(string(row, "type")),
                        number(row, "errorCount").intValue(),
                        number(row, "correctAnswer").intValue()
                ))
                .toList();

        Map<LocalDate, Map<String, Object>> dailyRows = new HashMap<>();
        for (Map<String, Object> row : reportMapper.dailyStats(userId, start, end)) {
            dailyRows.put(localDate(lookup(row, "practiceDate")), row);
        }
        List<WeeklyDailyPoint> daily = new ArrayList<>();
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = weekStart.plusDays(offset);
            Map<String, Object> row = dailyRows.get(date);
            daily.add(new WeeklyDailyPoint(
                    date.format(DAY_LABEL),
                    row == null ? 0 : number(row, "questionCount").longValue(),
                    row == null ? BigDecimal.ZERO : decimal(row, "accuracy")
            ));
        }

        RecommendedPractice recommendation = recommend(weakTypes, overview.questionCount());
        return new WeeklyFacts(
                weekStart,
                weekEnd,
                overview,
                weakTypes,
                mistakes,
                daily,
                calcStreak(statsMapper.submittedDates(userId)),
                recommendation
        );
    }

    private RecommendedPractice recommend(List<WeeklyTypeStat> weakTypes, long questionCount) {
        if (weakTypes.isEmpty()) {
            PracticeConfig config = new PracticeConfig(
                    "基础乘除巩固",
                    20,
                    List.of(QuestionType.TABLE_MULTIPLICATION, QuestionType.TABLE_DIVISION),
                    3,
                    false,
                    100,
                    20
            );
            String reason = questionCount == 0
                    ? "本周暂无足够练习数据，先用表内乘除建立稳定节奏。"
                    : "本周各题型样本还不足 3 道，先用表内乘除巩固基础。";
            return new RecommendedPractice(
                    config,
                    Map.of(
                            QuestionType.TABLE_MULTIPLICATION, 10,
                            QuestionType.TABLE_DIVISION, 10
                    ),
                    reason
            );
        }

        List<QuestionType> types = weakTypes.stream()
                .limit(2)
                .map(item -> parseType(item.type()))
                .toList();
        int total = 20;
        int firstCount = types.size() == 1 ? total : 12;
        Map<QuestionType, Integer> quotas = new LinkedHashMap<>();
        quotas.put(types.get(0), firstCount);
        if (types.size() > 1) {
            quotas.put(types.get(1), total - firstCount);
        }
        PracticeConfig config = new PracticeConfig(
                "周报薄弱题型提升",
                total,
                types,
                types.contains(QuestionType.MIXED_OPERATIONS) ? 3 : 2,
                types.contains(QuestionType.MIXED_OPERATIONS),
                100,
                20
        );
        String reason = types.size() == 1
                ? "本周最薄弱的是" + typeLabel(types.get(0).name()) + "，建议集中完成 20 道同标签练习。"
                : "建议将 60% 题量用于" + typeLabel(types.get(0).name())
                + "，40% 用于" + typeLabel(types.get(1).name()) + "。";
        return new RecommendedPractice(config, quotas, reason);
    }

    private int calcStreak(List<LocalDate> rawDates) {
        if (rawDates == null || rawDates.isEmpty()) {
            return 0;
        }
        Set<LocalDate> dates = new HashSet<>(rawDates);
        LocalDate cursor = LocalDate.now(ZONE);
        if (!dates.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        if (!dates.contains(cursor)) {
            return 0;
        }
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private QuestionType parseType(String type) {
        try {
            return QuestionType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return QuestionType.TABLE_MULTIPLICATION;
        }
    }

    private String typeLabel(String type) {
        return parseType(type).getLabel();
    }

    private Map<String, Object> nullSafe(Map<String, Object> row) {
        return row == null ? Map.of() : row;
    }

    private Object lookup(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace("_", "");
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String candidate = entry.getKey().toLowerCase(Locale.ROOT).replace("_", "");
            if (candidate.equals(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Number number(Map<String, Object> row, String key) {
        Object value = lookup(row, key);
        if (value instanceof Number number) {
            return number;
        }
        return 0;
    }

    private BigDecimal decimal(Map<String, Object> row, String key) {
        Object value = lookup(row, key);
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String string(Map<String, Object> row, String key) {
        Object value = lookup(row, key);
        return value == null ? "" : value.toString();
    }

    private LocalDate localDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return new java.sql.Date(date.getTime()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
