package com.bookease.reminder;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ReminderResponse", description = "A reminder for an appointment.")
public record ReminderResponse(
        Long id,
        Long appointmentId,
        Long userId,
        Instant reminderAt,
        ReminderChannel channel,
        ReminderStatus status,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt
) {
}
