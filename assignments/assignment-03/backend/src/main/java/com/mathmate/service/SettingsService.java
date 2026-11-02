package com.mathmate.service;

import com.mathmate.dto.settings.AccountProfileView;
import com.mathmate.dto.settings.AvatarPresignRequest;
import com.mathmate.dto.settings.AvatarStoredObject;
import com.mathmate.dto.settings.AvatarUploadConfig;
import com.mathmate.dto.settings.AvatarUploadTicket;
import com.mathmate.dto.settings.ChangePasswordRequest;
import com.mathmate.dto.settings.UpdateProfileRequest;
import com.mathmate.entity.User;
import com.mathmate.exception.BusinessException;
import com.mathmate.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

    public SettingsService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AvatarStorageService avatarStorageService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorageService = avatarStorageService;
    }

    public AccountProfileView getUserProfile(Long userId) {
        return toProfile(requireUser(userId));
    }

    @Transactional
    public AccountProfileView updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        String displayName = request.displayName().trim();
        if (displayName.isEmpty()) {
            throw new BusinessException("昵称不能为空");
        }
        userMapper.updateDisplayName(userId, displayName);
        user.setDisplayName(displayName);
        return toProfile(user);
    }

    public AvatarUploadConfig avatarConfig() {
        return avatarStorageService.config();
    }

    public AvatarUploadTicket createAvatarUploadTicket(Long userId, AvatarPresignRequest request) {
        requireUser(userId);
        return avatarStorageService.presign(userId, request);
    }

    @Transactional
    public AccountProfileView confirmOssAvatar(Long userId, String objectKey) {
        User user = requireUser(userId);
        AvatarStoredObject stored = avatarStorageService.confirmOss(userId, objectKey);
        return updateAvatar(user, stored);
    }

    @Transactional
    public AccountProfileView storeLocalAvatar(Long userId, MultipartFile file) {
        User user = requireUser(userId);
        AvatarStoredObject stored = avatarStorageService.storeLocal(userId, file);
        return updateAvatar(user, stored);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        verifyPassword(user, request.currentPassword());
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }
        userMapper.updatePasswordHash(userId, passwordEncoder.encode(request.newPassword()));
    }

    private User requireUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    private void verifyPassword(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException("当前密码不正确");
        }
    }

    private AccountProfileView updateAvatar(User user, AvatarStoredObject stored) {
        String oldKey = user.getAvatarObjectKey();
        userMapper.updateAvatar(user.getId(), stored.publicUrl(), stored.objectKey());
        user.setAvatarUrl(stored.publicUrl());
        user.setAvatarObjectKey(stored.objectKey());
        if (oldKey != null && !oldKey.equals(stored.objectKey())) {
            avatarStorageService.deleteStored(oldKey);
        }
        return toProfile(user);
    }

    private AccountProfileView toProfile(User user) {
        return new AccountProfileView(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl(), user.getCreatedAt()
        );
    }
}
