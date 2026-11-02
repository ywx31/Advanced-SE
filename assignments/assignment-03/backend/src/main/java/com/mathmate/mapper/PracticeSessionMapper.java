package com.mathmate.mapper;

import com.mathmate.entity.PracticeSession;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PracticeSessionMapper {
    int insert(PracticeSession session);

    int insertWithSubmission(PracticeSession session);

    PracticeSession findByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    long countByUser(@Param("userId") Long userId);

    List<PracticeSession> findPage(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    List<PracticeSession> findRecent(@Param("userId") Long userId, @Param("limit") int limit);

    int updateSubmission(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("correctCount") int correctCount,
            @Param("accuracy") BigDecimal accuracy,
            @Param("durationSeconds") int durationSeconds,
            @Param("submittedAt") LocalDateTime submittedAt
    );
}
