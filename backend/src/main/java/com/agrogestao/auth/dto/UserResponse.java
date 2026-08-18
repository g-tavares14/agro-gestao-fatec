package com.agrogestao.auth.dto;

import com.agrogestao.domain.User;
import com.agrogestao.domain.enums.AuthProvider;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        AuthProvider authProvider
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getAuthProvider());
    }
}
