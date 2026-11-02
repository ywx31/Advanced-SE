package com.mathmate.dto.wrongbook;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WrongPracticeRequest(
        @Size(max = 100, message = "单次最多选择 100 道错题")
        List<Long> wrongQuestionIds,
        @NotNull(message = "请选择练习模式")
        WrongPracticeMode mode,
        @Min(value = 1, message = "题目数量至少为 1")
        @Max(value = 100, message = "题目数量最多为 100")
        Integer count
) {
}
