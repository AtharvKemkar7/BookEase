package com.bookease.availability;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(name = "BreakResponse", description = "A provider break window (UTC).")
public record BreakResponse(
        Long id,
        Long providerId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {
}
