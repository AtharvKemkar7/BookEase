package com.bookease.service;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "ProviderServiceResponse", description = "A provider service offering.")
public record ProviderServiceResponse(
        Long id,
        Long providerId,
        String name,
        String description,
        int durationMinutes,
        BigDecimal price,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
