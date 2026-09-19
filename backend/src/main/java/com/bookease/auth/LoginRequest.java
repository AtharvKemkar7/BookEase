package com.bookease.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credentials for obtaining a JWT access token.")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "casey@example.com")
        @NotBlank @Email String email,
        @Schema(description = "Account password", example = "secret123")
        @NotBlank String password
) {
}
