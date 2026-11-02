package com.mathmate.mapper;

import com.mathmate.dto.stats.OverviewStats;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface StatsMapper {
    OverviewStats overview(@Param("userId") Long userId);

    List<Map<String, Object>> sessionTrend(@Param("userId") Long userId, @Param("limit") int limit);

    List<Map<String, Object>> typeAccuracy(@Param("userId") Long userId);

    List<Map<String, Object>> dailyStats(@Param("userId") Long userId, @Param("days") int days);

    List<Map<String, Object>> activityStats(
            @Param("userId") Long userId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    List<java.time.LocalDate> submittedDates(@Param("userId") Long userId);
}
