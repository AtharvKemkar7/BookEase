package com.bookease.availability;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AvailabilitySlot", description = "A bookable UTC time slot.")
public record AvailabilitySlot(
        Instant startAt,
        Instant endAt
) {
}
