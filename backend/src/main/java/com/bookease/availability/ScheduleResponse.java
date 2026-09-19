package com.bookease.availability;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(name = "ScheduleResponse", description = "A provider working schedule window (UTC).")
public record ScheduleResponse(
        Long id,
        Long providerId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {
}
