package com.bookease.service;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(name = "ProviderServiceRequest", description = "Create or update payload for a provider service offering.")
public record ProviderServiceRequest(
        @Schema(description = "Service name", example = "Haircut")
        @NotBlank @Size(max = 150) String name,
        @Schema(description = "Description") @Size(max = 1000) String description,
        @Schema(description = "Duration in minutes", example = "30")
        @NotNull @Min(5) @Max(1440) Integer durationMinutes,
        @Schema(description = "Price", example = "25.00")
        @NotNull @DecimalMin(value = "0.0") BigDecimal price,
        @Schema(description = "Whether the service is bookable", example = "true")
        Boolean active
) {
}
