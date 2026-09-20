package com.bookease.provider;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ProviderResponse", description = "Full provider profile (owner or admin view).")
public record ProviderResponse(
        Long id,
        Long userId,
        String businessName,
        String description,
        String address,
        String city,
        String phone,
        ProviderStatus status,
        Long categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt
) {
}
