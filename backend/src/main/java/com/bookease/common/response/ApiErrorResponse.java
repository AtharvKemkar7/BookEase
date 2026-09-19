package com.bookease.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiErrorResponse", description = "Structured, machine-readable error returned by every failing operation.")
public record ApiErrorResponse(
        @Schema(description = "Error timestamp (UTC)") Instant timestamp,
        @Schema(description = "Correlation id for request tracing") String correlationId,
        @Schema(description = "HTTP status code", example = "409") int status,
        @Schema(description = "Stable machine-readable error code", example = "EMAIL_ALREADY_REGISTERED") String code,
        @Schema(description = "Human-readable message", example = "Email already registered") String message,
        @Schema(description = "Request path", example = "/api/v1/auth/register") String path,
        @Schema(description = "Optional field-level validation details") List<String> details
) {
}
