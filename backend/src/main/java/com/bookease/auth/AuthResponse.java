package com.bookease.auth;

import com.bookease.user.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponse", description = "JWT access token issued after a successful login.")
public record AuthResponse(
        @Schema(description = "Signed JWT access token") String accessToken,
        @Schema(description = "Token type used in the Authorization header", example = "Bearer") String tokenType,
        @Schema(description = "Token lifetime in seconds", example = "3600") long expiresIn,
        @Schema(description = "Authenticated user") UserResponse user
) {
}
