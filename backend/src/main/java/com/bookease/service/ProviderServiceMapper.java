package com.bookease.service;

public final class ProviderServiceMapper {

    private ProviderServiceMapper() {
    }

    public static ProviderServiceResponse toResponse(ProviderService service) {
        return new ProviderServiceResponse(
                service.getId(),
                service.getProvider().getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPrice(),
                service.isActive(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }
}
