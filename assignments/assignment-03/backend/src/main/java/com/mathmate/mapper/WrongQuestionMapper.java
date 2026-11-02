package com.mathmate.mapper;

import com.mathmate.entity.WrongQuestion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WrongQuestionMapper {
    WrongQuestion findByUserAndKey(@Param("userId") Long userId, @Param("canonicalKey") String canonicalKey);

    WrongQuestion findByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    List<WrongQuestion> findByIdsAndUser(
            @Param("userId") Long userId,
            @Param("ids") List<Long> ids
    );

    List<WrongQuestion> findActiveForPractice(@Param("userId") Long userId);

    List<String> findAllCanonicalKeys(@Param("userId") Long userId);

    int insert(WrongQuestion wrongQuestion);

    int updateAfterWrong(WrongQuestion wrongQuestion);

    int updateAfterCorrect(
            @Param("id") Long id,
            @Param("correctStreak") int correctStreak,
            @Param("mastered") boolean mastered,
            @Param("lastPracticedAt") java.time.LocalDateTime lastPracticedAt
    );

    long countPage(@Param("userId") Long userId, @Param("mastered") Boolean mastered);

    List<WrongQuestion> findPage(
            @Param("userId") Long userId,
            @Param("mastered") Boolean mastered,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countActive(@Param("userId") Long userId);

    int updateMastered(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("mastered") boolean mastered
    );
}
