package com.mathmate.mapper;

import com.mathmate.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    int insert(User user);

    int updateDisplayName(@Param("id") Long id, @Param("displayName") String displayName);

    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateAvatar(
            @Param("id") Long id,
            @Param("avatarUrl") String avatarUrl,
            @Param("avatarObjectKey") String avatarObjectKey
    );
}
