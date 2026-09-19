package com.bookease.availability;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(name = "BreakRequest", description = "Provider break window (UTC).")
public record BreakRequest(
        @Schema(description = "Day of week", example = "MONDAY")
        @NotNull DayOfWeek dayOfWeek,
        @Schema(description = "Start time (UTC)", example = "13:00")
        @NotNull LocalTime startTime,
        @Schema(description = "End time (UTC)", example = "14:00")
        @NotNull LocalTime endTime,
        @Schema(description = "Whether the break is active", example = "true")
        Boolean active
) {
}
