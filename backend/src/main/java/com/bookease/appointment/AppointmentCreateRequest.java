package com.bookease.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(name = "AppointmentCreateRequest", description = "Booking request. Ownership comes from the JWT, never from the body.")
public record AppointmentCreateRequest(
        @Schema(description = "Provider id", example = "1")
        @NotNull Long providerId,
        @Schema(description = "Service id", example = "1")
        @NotNull Long serviceId,
        @Schema(description = "Start time (UTC ISO-8601)", example = "2026-06-01T10:00:00Z")
        @NotNull Instant startAt,
        @Schema(description = "Optional notes")
        @Size(max = 1000) String notes
) {
}
