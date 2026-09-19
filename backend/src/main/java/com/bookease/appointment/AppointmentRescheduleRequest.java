package com.bookease.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(name = "AppointmentRescheduleRequest", description = "Reschedule request; the new start time is fully revalidated.")
public record AppointmentRescheduleRequest(
        @Schema(description = "New start time (UTC ISO-8601)", example = "2026-06-02T10:00:00Z")
        @NotNull Instant startAt,
        @Schema(description = "Optional updated notes")
        @Size(max = 1000) String notes
) {
}
