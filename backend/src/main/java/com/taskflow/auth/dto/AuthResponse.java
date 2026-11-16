package com.taskflow.auth.dto;

import com.taskflow.user.dto.UserResponse;

/**
 * @param accessToken short-lived JWT, sent back as {@code Authorization: Bearer ...}
 * @param expiresIn   access-token lifetime in seconds
 * @param user        profile of the authenticated user
 */
public record AuthResponse(String accessToken, long expiresIn, UserResponse user) {
}
