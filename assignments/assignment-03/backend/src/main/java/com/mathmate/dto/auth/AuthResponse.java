package com.mathmate.dto.auth;

public record AuthResponse(String token, UserProfile user) {
}
