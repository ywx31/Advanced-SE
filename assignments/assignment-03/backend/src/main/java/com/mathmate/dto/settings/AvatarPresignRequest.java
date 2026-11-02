package com.mathmate.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AvatarPresignRequest(
        @NotBlank(message = "文件名不能为空") String fileName,
        @NotBlank(message = "文件类型不能为空") String contentType,
        @Positive(message = "文件大小不正确") long size
) {
}
