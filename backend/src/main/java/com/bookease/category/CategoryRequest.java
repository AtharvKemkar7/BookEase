package com.bookease.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CategoryRequest", description = "Create or update payload for a category.")
public record CategoryRequest(
        @Schema(description = "Category name", example = "Dentist")
        @NotBlank @Size(max = 120) String name,
        @Schema(description = "Optional description", example = "Dental care providers")
        @Size(max = 500) String description,
        @Schema(description = "Whether the category is active", example = "true")
        Boolean active
) {
}
