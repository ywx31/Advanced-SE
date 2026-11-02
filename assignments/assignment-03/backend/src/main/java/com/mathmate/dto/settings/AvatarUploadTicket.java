package com.mathmate.dto.settings;

import java.util.Map;

public record AvatarUploadTicket(
        String mode,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        String objectKey,
        String publicUrl,
        long expiresAtEpochSeconds
) {
}
