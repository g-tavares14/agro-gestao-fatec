package com.agrogestao.auth.dto;

public record AuthResponse(String token, UserResponse user) {
}
