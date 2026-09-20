package com.bookease.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "UserResponse", description = "Public representation of a user. Never includes password data.")
public record UserResponse(
        @Schema(description = "User identifier", example = "1") Long id,
        @Schema(description = "Display name", example = "Casey User") String name,
        @Schema(description = "Normalized email address", example = "casey@example.com") String email,
        @Schema(description = "Contact phone number", example = "+10000000001") String phone,
        @Schema(description = "Assigned role") UserRole role,
        @Schema(description = "Account status") UserStatus status,
        @Schema(description = "Creation timestamp (UTC)") Instant createdAt,
        @Schema(description = "Last update timestamp (UTC)") Instant updatedAt
) {
}
