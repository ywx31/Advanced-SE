package com.mathmate.service;

import com.mathmate.dto.practice.QuestionType;
import com.mathmate.dto.stats.ActivityDay;
import com.mathmate.dto.stats.DailyPoint;
import com.mathmate.dto.stats.DashboardResponse;
import com.mathmate.dto.stats.OverviewStats;
import com.mathmate.dto.stats.TrendPoint;
import com.mathmate.dto.stats.TypeAccuracy;
import com.mathmate.mapper.StatsMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class StatsService {
    private final StatsMapper statsMapper;
    private final PracticeService practiceService;

    public StatsService(StatsMapper statsMapper, PracticeService practiceService) {
        this.statsMapper = statsMapper;
        this.practiceService = practiceService;
    }

    public DashboardResponse dashboard(Long userId) {
        OverviewStats overview = statsMapper.overview(userId);
        if (overview == null) {
            overview = new OverviewStats();
        }
        List<TrendPoint> trend = new ArrayList<>();
        for (Map<String, Object> row : statsMapper.sessionTrend(userId, 20)) {
            trend.add(new TrendPoint(
                    number(row, "sessionId").longValue(),
                    string(row, "label"),
                    decimal(row, "accuracy"),
                    number(row, "correctCount").intValue(),
                    number(row, "totalCount").intValue()
            ));
        }
        Collections.reverse(trend);

        List<TypeAccuracy> typeAccuracy = statsMapper.typeAccuracy(userId).stream()
                .map(row -> {
                    String type = string(row, "type");
                    String label = enumLabel(type);
                    return new TypeAccuracy(
                            type,
                            label,
                            number(row, "total").longValue(),
                            number(row, "correct").longValue(),
                            decimal(row, "accuracy")
                    );
                })
                .toList();

        LocalDate activityEnd = LocalDate.now();
        LocalDate activityStart = activityEnd.minusDays(364);
        List<ActivityDay> activity = statsMapper.activityStats(userId, activityStart, activityEnd).stream()
                .map(row -> new ActivityDay(
                        localDate(lookup(row, "practiceDate")),
                        number(row, "sessionCount").longValue(),
                        number(row, "questionCount").longValue(),
                        decimal(row, "accuracy")
                ))
                .toList();

        List<DailyPoint> daily = statsMapper.dailyStats(userId, 13).stream()
                .map(row -> new DailyPoint(
                        string(row, "date"),
                        number(row, "sessionCount").longValue(),
                        number(row, "questionCount").longValue(),
                        decimal(row, "accuracy")
                ))
                .toList();

        return new DashboardResponse(
                overview,
                trend,
                typeAccuracy,
                daily,
                activity,
                practiceService.recentSessions(userId, 5),
                calcStreak(statsMapper.submittedDates(userId))
        );
    }

    private int calcStreak(List<LocalDate> rawDates) {
        if (rawDates == null || rawDates.isEmpty()) {
            return 0;
        }
        Set<LocalDate> dates = new HashSet<>(rawDates);
        LocalDate cursor = LocalDate.now();
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

    private String enumLabel(String type) {
        try {
            return QuestionType.valueOf(type).getLabel();
        } catch (IllegalArgumentException exception) {
            return type;
        }
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
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private String string(Map<String, Object> row, String key) {
        Object value = lookup(row, key);
        return value == null ? "" : value.toString();
    }

    private Object lookup(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String lowered = key.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key) || entry.getKey().toLowerCase(Locale.ROOT).equals(lowered)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
