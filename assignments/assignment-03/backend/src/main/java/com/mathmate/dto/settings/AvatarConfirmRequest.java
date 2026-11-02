package com.mathmate.dto.settings;

import jakarta.validation.constraints.NotBlank;

public record AvatarConfirmRequest(@NotBlank(message = "头像对象标识不能为空") String objectKey) {
}
