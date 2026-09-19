package com.bookease.availability;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(name = "AvailabilityResponse", description = "Available booking slots for a provider service on a date (UTC).")
public record AvailabilityResponse(
        Long providerId,
        Long serviceId,
        LocalDate date,
        int durationMinutes,
        List<AvailabilitySlot> slots
) {
}
