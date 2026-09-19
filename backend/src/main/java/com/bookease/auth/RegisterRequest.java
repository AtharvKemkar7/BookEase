package com.bookease.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest", description = "Payload for registering a USER account.")
public record RegisterRequest(
        @Schema(description = "Display name", example = "Casey User")
        @NotBlank @Size(max = 150) String name,
        @Schema(description = "Email address (normalized to lower case)", example = "casey@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "Password, 8-72 characters", example = "secret123")
        @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(description = "Optional phone number", example = "+10000000001")
        @Size(max = 32) String phone
) {
}
