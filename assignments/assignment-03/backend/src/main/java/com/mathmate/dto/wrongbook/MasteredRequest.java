package com.mathmate.dto.wrongbook;

import jakarta.validation.constraints.NotNull;

public record MasteredRequest(@NotNull Boolean mastered) {
}
