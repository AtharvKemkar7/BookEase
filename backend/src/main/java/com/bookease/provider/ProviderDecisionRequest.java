package com.bookease.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "ProviderDecisionRequest", description = "Optional reason supplied when rejecting a provider.")
public record ProviderDecisionRequest(
        @Schema(description = "Reason for the decision", example = "Incomplete business information")
        @Size(max = 500) String reason
) {
}
