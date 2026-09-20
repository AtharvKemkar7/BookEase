package com.bookease.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ProviderCreateRequest", description = "Onboarding payload for creating a provider profile.")
public record ProviderCreateRequest(
        @Schema(description = "Category id", example = "1")
        @NotNull Long categoryId,
        @Schema(description = "Business name", example = "ABC Salon")
        @NotBlank @Size(max = 150) String businessName,
        @Schema(description = "Description") @Size(max = 1000) String description,
        @Schema(description = "Street address") @Size(max = 255) String address,
        @Schema(description = "City", example = "Berlin") @Size(max = 120) String city,
        @Schema(description = "Contact phone") @Size(max = 32) String phone
) {
}
