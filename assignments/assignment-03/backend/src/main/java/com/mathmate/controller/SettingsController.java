package com.mathmate.controller;

import com.mathmate.common.ApiResponse;
import com.mathmate.dto.settings.AccountProfileView;
import com.mathmate.dto.settings.AvatarConfirmRequest;
import com.mathmate.dto.settings.AvatarPresignRequest;
import com.mathmate.dto.settings.AvatarUploadConfig;
import com.mathmate.dto.settings.AvatarUploadTicket;
import com.mathmate.dto.settings.ChangePasswordRequest;
import com.mathmate.dto.settings.UpdateProfileRequest;
import com.mathmate.security.SecurityUtils;
import com.mathmate.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ApiResponse<AccountProfileView> getSettings() {
        return ApiResponse.ok(settingsService.getUserProfile(SecurityUtils.currentUserId()));
    }

    @PutMapping("/profile")
    public ApiResponse<AccountProfileView> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.ok(
                settingsService.updateProfile(SecurityUtils.currentUserId(), request)
        );
    }

    @GetMapping("/avatar/config")
    public ApiResponse<AvatarUploadConfig> avatarConfig() {
        return ApiResponse.ok(settingsService.avatarConfig());
    }

    @PostMapping("/avatar/presign")
    public ApiResponse<AvatarUploadTicket> presignAvatar(
            @Valid @RequestBody AvatarPresignRequest request
    ) {
        return ApiResponse.ok(
                settingsService.createAvatarUploadTicket(SecurityUtils.currentUserId(), request)
        );
    }

    @PostMapping("/avatar/confirm")
    public ApiResponse<AccountProfileView> confirmAvatar(
            @Valid @RequestBody AvatarConfirmRequest request
    ) {
        return ApiResponse.ok(
                settingsService.confirmOssAvatar(SecurityUtils.currentUserId(), request.objectKey())
        );
    }

    @PostMapping("/avatar/local")
    public ApiResponse<AccountProfileView> uploadLocalAvatar(
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(
                settingsService.storeLocalAvatar(SecurityUtils.currentUserId(), file)
        );
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        settingsService.changePassword(SecurityUtils.currentUserId(), request);
        return ApiResponse.ok();
    }
}
