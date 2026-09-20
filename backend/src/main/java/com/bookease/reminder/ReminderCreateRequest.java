package com.bookease.reminder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(name = "ReminderCreateRequest", description = "Create a reminder for one of the caller's appointments.")
public record ReminderCreateRequest(
        @Schema(description = "Appointment id (must belong to the caller)", example = "1")
        @NotNull Long appointmentId,
        @Schema(description = "Reminder time (UTC ISO-8601)", example = "2026-06-01T09:00:00Z")
        @NotNull Instant reminderAt,
        @Schema(description = "Delivery channel", example = "IN_APP")
        @NotNull ReminderChannel channel
) {
}
