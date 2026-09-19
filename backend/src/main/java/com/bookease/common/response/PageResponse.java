package com.bookease.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(name = "PageResponse", description = "Pagination envelope for list endpoints.")
public record PageResponse<T>(
        @Schema(description = "Page content") List<T> content,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Page size") int size,
        @Schema(description = "Total number of elements") long totalElements,
        @Schema(description = "Total number of pages") int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
