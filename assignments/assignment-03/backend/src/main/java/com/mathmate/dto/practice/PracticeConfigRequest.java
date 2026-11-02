package com.mathmate.dto.practice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PracticeConfigRequest(
        @Size(max = 100, message = "标题最多 100 个字符") String title,
        @NotNull(message = "题目数量不能为空")
        @Min(value = 1, message = "至少生成 1 道题")
        @Max(value = 300, message = "单次最多生成 300 道题")
        Integer count,
        @NotEmpty(message = "至少选择一种题型") List<QuestionType> types,
        @Min(value = 2, message = "运算数至少为 2 个")
        @Max(value = 6, message = "运算数最多为 6 个")
        Integer operandCount,
        Boolean allowParentheses,
        @Min(value = 10, message = "数值上限不能小于 10")
        @Max(value = 1000, message = "数值上限不能超过 1000")
        Integer maxNumber,
        @Min(value = 9, message = "乘法因子上限不能小于 9")
        @Max(value = 50, message = "乘法因子上限不能超过 50")
        Integer maxFactor
) {
}
