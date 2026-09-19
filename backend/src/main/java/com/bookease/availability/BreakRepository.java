package com.bookease.availability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface BreakRepository extends JpaRepository<Break, Long> {

    List<Break> findByProviderIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long providerId);

    List<Break> findByProviderIdAndDayOfWeekAndActiveTrue(Long providerId, DayOfWeek dayOfWeek);

    Optional<Break> findByIdAndProviderId(Long id, Long providerId);

    long countByProviderId(Long providerId);
}
