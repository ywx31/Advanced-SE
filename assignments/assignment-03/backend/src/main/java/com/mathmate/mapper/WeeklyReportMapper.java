package com.mathmate.mapper;

import com.mathmate.entity.WeeklyReport;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface WeeklyReportMapper {
    int upsert(WeeklyReport report);

    WeeklyReport findByUserAndWeek(
            @Param("userId") Long userId,
            @Param("weekStart") LocalDate weekStart
    );

    WeeklyReport findLatest(@Param("userId") Long userId);

    WeeklyReport findByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    Map<String, Object> weeklySummary(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Map<String, Object>> dailyStats(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Map<String, Object>> typeStats(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Map<String, Object>> mistakeStats(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Long> findActiveUserIds(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
