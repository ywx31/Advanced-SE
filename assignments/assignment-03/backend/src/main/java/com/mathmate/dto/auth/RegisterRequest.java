package com.mathmate.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,30}$", message = "用户名需为 3-30 位字母、数字或下划线")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度需为 6-50 位")
        String password,
        @NotBlank(message = "昵称不能为空")
        @Size(max = 30, message = "昵称最多 30 个字符")
        String displayName
) {
}
