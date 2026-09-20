package com.bookease.provider;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProviderSummaryResponse", description = "Public provider summary used in discovery results.")
public record ProviderSummaryResponse(
        Long id,
        String businessName,
        String description,
        String city,
        Long categoryId,
        String categoryName
) {
}
