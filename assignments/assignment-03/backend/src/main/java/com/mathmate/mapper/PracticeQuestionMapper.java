package com.mathmate.mapper;

import com.mathmate.entity.PracticeQuestion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PracticeQuestionMapper {
    int insert(PracticeQuestion question);

    List<PracticeQuestion> findBySessionId(@Param("sessionId") Long sessionId);

    int updateAnswer(
            @Param("sessionId") Long sessionId,
            @Param("id") Long id,
            @Param("userAnswer") Integer userAnswer,
            @Param("correct") boolean correct
    );
}
