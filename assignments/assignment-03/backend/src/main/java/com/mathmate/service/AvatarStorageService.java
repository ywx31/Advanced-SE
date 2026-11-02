package com.mathmate.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.mathmate.dto.settings.AvatarPresignRequest;
import com.mathmate.dto.settings.AvatarStoredObject;
import com.mathmate.dto.settings.AvatarUploadConfig;
import com.mathmate.dto.settings.AvatarUploadTicket;
import com.mathmate.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class AvatarStorageService {
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final boolean ossEnabled;
    private final String endpoint;
    private final String bucket;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String publicBaseUrl;
    private final long expirationSeconds;
    private final long maxSize;
    private final Path localRoot;
    private final OSS ossClient;

    public AvatarStorageService(
            @Value("${app.avatar.oss.enabled:false}") boolean ossEnabled,
            @Value("${app.avatar.oss.endpoint:}") String endpoint,
            @Value("${app.avatar.oss.bucket:}") String bucket,
            @Value("${app.avatar.oss.access-key-id:}") String accessKeyId,
            @Value("${app.avatar.oss.access-key-secret:}") String accessKeySecret,
            @Value("${app.avatar.oss.public-base-url:}") String publicBaseUrl,
            @Value("${app.avatar.oss.upload-expiration-seconds:600}") long expirationSeconds,
            @Value("${app.avatar.max-size:5242880}") long maxSize,
            @Value("${app.avatar.local-root:uploads}") String localRoot
    ) {
        this.ossEnabled = ossEnabled;
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.publicBaseUrl = publicBaseUrl;
        this.expirationSeconds = expirationSeconds;
        this.maxSize = maxSize;
        this.localRoot = Path.of(localRoot).toAbsolutePath().normalize();
        if (ossEnabled) {
            if (endpoint.isBlank() || bucket.isBlank()
                    || accessKeyId.isBlank() || accessKeySecret.isBlank()) {
                throw new IllegalStateException("OSS_ENABLED=true 时必须配置 endpoint、bucket、AccessKey ID 和 Secret");
            }
            this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        } else {
            this.ossClient = null;
        }
    }

    public AvatarUploadConfig config() {
        return new AvatarUploadConfig(ossEnabled ? "OSS" : "LOCAL", maxSize);
    }

    public AvatarUploadTicket presign(Long userId, AvatarPresignRequest request) {
        requireOss();
        validate(request.contentType(), request.size());
        String objectKey = "avatars/" + userId + "/" + UUID.randomUUID() + "." + extension(request.contentType());
        Date expiration = new Date(Instant.now().plusSeconds(expirationSeconds).toEpochMilli());

        GeneratePresignedUrlRequest presign = new GeneratePresignedUrlRequest(
                bucket, objectKey, HttpMethod.PUT
        );
        presign.setExpiration(expiration);
        presign.setContentType(request.contentType());
        presign.addHeader("Content-Disposition", "inline");
        presign.addHeader("Cache-Control", "public, max-age=31536000, immutable");
        String uploadUrl = ossClient.generatePresignedUrl(presign).toString();
        return new AvatarUploadTicket(
                "OSS",
                uploadUrl,
                "PUT",
                Map.of(
                        "Content-Type", request.contentType(),
                        "Content-Disposition", "inline",
                        "Cache-Control", "public, max-age=31536000, immutable"
                ),
                objectKey,
                publicUrl(objectKey),
                expiration.toInstant().getEpochSecond()
        );
    }

    public AvatarStoredObject confirmOss(Long userId, String objectKey) {
        requireOss();
        if (objectKey == null || !objectKey.startsWith("avatars/" + userId + "/")) {
            throw new BusinessException("头像对象标识不合法");
        }
        try {
            if (!ossClient.doesObjectExist(bucket, objectKey)) {
                throw new BusinessException("头像上传尚未完成");
            }
            ObjectMetadata metadata = ossClient.getObjectMetadata(bucket, objectKey);
            validate(metadata.getContentType(), metadata.getContentLength());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("无法确认云端头像，请重新上传");
        }
        return new AvatarStoredObject(publicUrl(objectKey), objectKey);
    }

    public AvatarStoredObject storeLocal(Long userId, MultipartFile file) {
        validate(file.getContentType(), file.getSize());
        String extension = extension(file.getContentType());
        String relative = "avatars/" + userId + "/" + UUID.randomUUID() + "." + extension;
        Path target = localRoot.resolve(relative).normalize();
        if (!target.startsWith(localRoot.resolve("avatars"))) {
            throw new BusinessException("头像保存路径不合法");
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException exception) {
            throw new BusinessException("头像保存失败，请稍后重试");
        }
        return new AvatarStoredObject("/uploads/" + relative, "local:" + relative);
    }

    public void deleteStored(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            if (objectKey.startsWith("local:")) {
                Path file = localRoot.resolve(objectKey.substring("local:".length())).normalize();
                if (file.startsWith(localRoot)) {
                    Files.deleteIfExists(file);
                }
            } else if (ossEnabled) {
                ossClient.deleteObject(bucket, objectKey);
            }
        } catch (Exception ignored) {
            // 旧头像清理失败不应阻断新头像保存。
        }
    }

    private void validate(String contentType, long size) {
        if (contentType == null || !ALLOWED_TYPES.containsKey(contentType.toLowerCase())) {
            throw new BusinessException("头像仅支持 JPG、PNG 或 WebP 格式");
        }
        if (size <= 0 || size > maxSize) {
            throw new BusinessException("头像大小不能超过 " + (maxSize / 1024 / 1024) + " MB");
        }
    }

    private String extension(String contentType) {
        return ALLOWED_TYPES.get(contentType.toLowerCase());
    }

    private String publicUrl(String objectKey) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (!base.isBlank()) {
            return base.replaceAll("/+$", "") + "/" + objectKey;
        }
        URI uri = URI.create(endpoint);
        String host = uri.getHost();
        String scheme = uri.getScheme() == null ? "https" : uri.getScheme();
        return scheme + "://" + bucket + "." + host + "/" + objectKey;
    }

    private void requireOss() {
        if (!ossEnabled) {
            throw new BusinessException("OSS 尚未配置，请使用本地存储模式");
        }
    }
}
