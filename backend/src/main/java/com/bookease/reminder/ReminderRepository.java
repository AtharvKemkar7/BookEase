package com.bookease.reminder;

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

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    Page<Reminder> findByUserIdOrderByReminderAtDesc(Long userId, Pageable pageable);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    boolean existsByAppointmentIdAndReminderAtAndChannel(
            Long appointmentId, Instant reminderAt, ReminderChannel channel);

    long countByAppointmentIdAndStatus(Long appointmentId, ReminderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from Reminder r
            where r.status = com.bookease.reminder.ReminderStatus.PENDING
              and r.reminderAt <= :now
            order by r.reminderAt asc
            """)
    List<Reminder> findDueForUpdate(@Param("now") Instant now, Pageable pageable);

    List<Reminder> findByAppointmentIdAndStatus(Long appointmentId, ReminderStatus status);
}
