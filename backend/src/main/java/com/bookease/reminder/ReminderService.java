package com.bookease.reminder;

import com.bookease.appointment.Appointment;
import com.bookease.appointment.AppointmentRepository;
import com.bookease.appointment.AppointmentStatus;
import com.bookease.common.exception.BusinessException;
import com.bookease.common.exception.ErrorCode;
import com.bookease.common.response.PageResponse;
import com.bookease.security.CurrentUser;
import com.bookease.user.User;
import com.bookease.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public ReminderService(
            ReminderRepository reminderRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            CurrentUser currentUser) {
        this.reminderRepository = reminderRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public ReminderResponse create(ReminderCreateRequest request) {
        Long userId = currentUser.requireUserId();
        Appointment appointment = appointmentRepository.findByIdAndUserId(request.appointmentId(), userId)
                .orElseThrow(() -> notFound("Appointment not found"));

        if (appointment.getStatus().isTerminal()) {
            throw new BusinessException(
                    ErrorCode.REMINDER_CONFLICT,
                    HttpStatus.CONFLICT,
                    "Reminders can only be created for pending or confirmed appointments");
        }
        if (!request.reminderAt().isAfter(Instant.now())) {
            throw new BusinessException(
                    ErrorCode.REMINDER_IN_PAST, HttpStatus.CONFLICT, "Reminder time must be in the future");
        }
        if (!request.reminderAt().isBefore(appointment.getStartAt())) {
            throw new BusinessException(
                    ErrorCode.REMINDER_CONFLICT,
                    HttpStatus.CONFLICT,
                    "Reminder time must be before the appointment start time");
        }
        if (reminderRepository.existsByAppointmentIdAndReminderAtAndChannel(
                appointment.getId(), request.reminderAt(), request.channel())) {
            throw duplicateReminder();
        }

        User user = userRepository.getReferenceById(userId);
        Reminder reminder = new Reminder();
        reminder.setAppointment(appointment);
        reminder.setUser(user);
        reminder.setReminderAt(request.reminderAt());
        reminder.setChannel(request.channel());
        reminder.setStatus(ReminderStatus.PENDING);

        try {
            return ReminderMapper.toResponse(reminderRepository.saveAndFlush(reminder));
        } catch (DataIntegrityViolationException ex) {
            throw duplicateReminder();
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ReminderResponse> listMine(Pageable pageable) {
        Long userId = currentUser.requireUserId();
        Page<ReminderResponse> page = reminderRepository
                .findByUserIdOrderByReminderAtDesc(userId, pageable)
                .map(ReminderMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional
    public void cancel(Long reminderId) {
        Long userId = currentUser.requireUserId();
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> notFound("Reminder not found"));
        if (reminder.getStatus() != ReminderStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.REMINDER_CONFLICT,
                    HttpStatus.CONFLICT,
                    "Only pending reminders can be cancelled");
        }
        reminder.setStatus(ReminderStatus.CANCELLED);
        reminderRepository.saveAndFlush(reminder);
    }

    private static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    private static BusinessException duplicateReminder() {
        return new BusinessException(
                ErrorCode.DUPLICATE_REMINDER,
                HttpStatus.CONFLICT,
                "An identical reminder already exists");
    }
}
