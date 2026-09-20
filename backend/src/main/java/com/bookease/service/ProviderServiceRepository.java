package com.bookease.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderServiceRepository extends JpaRepository<ProviderService, Long> {

    List<ProviderService> findByProviderIdOrderByNameAsc(Long providerId);

    List<ProviderService> findByProviderIdAndActiveTrueOrderByNameAsc(Long providerId);

    Optional<ProviderService> findByIdAndProviderId(Long id, Long providerId);

    boolean existsByProviderIdAndNameKeyAndActiveTrue(Long providerId, String nameKey);

    boolean existsByProviderIdAndNameKeyAndActiveTrueAndIdNot(Long providerId, String nameKey, Long id);
}
