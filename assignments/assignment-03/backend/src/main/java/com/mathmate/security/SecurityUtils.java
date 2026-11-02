package com.mathmate.security;

import com.mathmate.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException("登录状态已失效，请重新登录", HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentUser().id();
    }
}
