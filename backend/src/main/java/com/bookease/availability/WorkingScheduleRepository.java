package com.bookease.availability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface WorkingScheduleRepository extends JpaRepository<WorkingSchedule, Long> {

    List<WorkingSchedule> findByProviderIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long providerId);

    List<WorkingSchedule> findByProviderIdAndDayOfWeekAndActiveTrue(Long providerId, DayOfWeek dayOfWeek);

    Optional<WorkingSchedule> findByIdAndProviderId(Long id, Long providerId);

    long countByProviderId(Long providerId);
}
