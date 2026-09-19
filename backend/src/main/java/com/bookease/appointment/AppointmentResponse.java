package com.bookease.appointment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AppointmentResponse", description = "An appointment. Ownership is derived from the JWT.")
public record AppointmentResponse(
        Long id,
        Long userId,
        Long providerId,
        String providerName,
        Long serviceId,
        String serviceName,
        Instant startAt,
        Instant endAt,
        AppointmentStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
