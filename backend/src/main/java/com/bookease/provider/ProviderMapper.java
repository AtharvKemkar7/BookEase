package com.bookease.provider;

public final class ProviderMapper {

    private ProviderMapper() {
    }

    public static ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getUser().getId(),
                provider.getBusinessName(),
                provider.getDescription(),
                provider.getAddress(),
                provider.getCity(),
                provider.getPhone(),
                provider.getStatus(),
                provider.getCategory().getId(),
                provider.getCategory().getName(),
                provider.getCreatedAt(),
                provider.getUpdatedAt());
    }

    public static ProviderSummaryResponse toSummary(Provider provider) {
        return new ProviderSummaryResponse(
                provider.getId(),
                provider.getBusinessName(),
                provider.getDescription(),
                provider.getCity(),
                provider.getCategory().getId(),
                provider.getCategory().getName());
    }
}
