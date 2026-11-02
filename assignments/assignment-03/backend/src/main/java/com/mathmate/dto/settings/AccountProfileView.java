package com.mathmate.dto.settings;

import java.time.LocalDateTime;

public record AccountProfileView(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        LocalDateTime createdAt
) {
}
