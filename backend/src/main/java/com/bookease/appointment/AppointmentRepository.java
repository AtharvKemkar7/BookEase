package com.bookease.appointment;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByIdAndUserId(Long id, Long userId);

    Optional<Appointment> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Page<Appointment> findByUserIdOrderByStartAtDesc(Long userId, Pageable pageable);

    Page<Appointment> findByProviderIdOrderByStartAtDesc(Long providerId, Pageable pageable);

    @Query("""
            select count(a) from Appointment a
            where a.provider.id = :providerId
              and a.status in (com.bookease.appointment.AppointmentStatus.PENDING,
                               com.bookease.appointment.AppointmentStatus.CONFIRMED)
              and a.startAt < :endAt
              and a.endAt > :startAt
              and (:excludeId is null or a.id <> :excludeId)
            """)
    long countOverlapping(
            @Param("providerId") Long providerId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludeId") Long excludeId);

    @Query("""
            select a from Appointment a
            where a.provider.id = :providerId
              and a.status in (com.bookease.appointment.AppointmentStatus.PENDING,
                               com.bookease.appointment.AppointmentStatus.CONFIRMED)
              and a.endAt > :from
              and a.startAt < :to
            order by a.startAt asc
            """)
    List<Appointment> findOccupied(@Param("providerId") Long providerId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);

    boolean existsByProviderIdAndStatusIn(Long providerId,
                                          java.util.Collection<AppointmentStatus> statuses);
}
