package com.mathmate.dto.practice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitPracticeRequest(@NotNull List<@Valid AnswerItem> answers) {
}
