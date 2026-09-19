package com.bookease.availability;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(name = "ScheduleRequest", description = "Working schedule window for a provider (UTC).")
public record ScheduleRequest(
        @Schema(description = "Day of week", example = "MONDAY")
        @NotNull DayOfWeek dayOfWeek,
        @Schema(description = "Start time (UTC)", example = "09:00")
        @NotNull LocalTime startTime,
        @Schema(description = "End time (UTC)", example = "17:00")
        @NotNull LocalTime endTime,
        @Schema(description = "Whether the window is active", example = "true")
        Boolean active
) {
}
