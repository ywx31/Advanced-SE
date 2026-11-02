package com.mathmate.service;

import com.mathmate.dto.auth.AuthResponse;
import com.mathmate.dto.auth.LoginRequest;
import com.mathmate.dto.auth.RegisterRequest;
import com.mathmate.dto.auth.UserProfile;
import com.mathmate.entity.User;
import com.mathmate.exception.BusinessException;
import com.mathmate.mapper.UserMapper;
import com.mathmate.security.JwtService;
import com.mathmate.security.SecurityUtils;
import com.mathmate.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        userMapper.insert(user);
        return createAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.username().trim());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        return createAuthResponse(user);
    }

    public UserProfile me() {
        UserPrincipal principal = SecurityUtils.currentUser();
        User user = userMapper.findById(principal.id());
        if (user == null) {
            throw new BusinessException("用户不存在", HttpStatus.UNAUTHORIZED);
        }
        return new UserProfile(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
    }

    private AuthResponse createAuthResponse(User user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getDisplayName());
        return new AuthResponse(
                jwtService.createToken(principal),
                new UserProfile(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl())
        );
    }
}
