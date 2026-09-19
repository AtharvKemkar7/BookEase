package com.bookease.appointment;

import com.bookease.availability.AvailabilityService;
import com.bookease.common.exception.BusinessException;
import com.bookease.common.exception.ErrorCode;
import com.bookease.common.response.PageResponse;
import com.bookease.provider.Provider;
import com.bookease.provider.ProviderRepository;
import com.bookease.reminder.Reminder;
import com.bookease.reminder.ReminderRepository;
import com.bookease.reminder.ReminderStatus;
import com.bookease.security.AuthenticatedUser;
import com.bookease.security.CurrentUser;
import com.bookease.service.ProviderService;
import com.bookease.user.User;
import com.bookease.user.UserRepository;
import com.bookease.user.UserRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final ReminderRepository reminderRepository;
    private final AvailabilityService availabilityService;
    private final CurrentUser currentUser;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            ProviderRepository providerRepository,
            UserRepository userRepository,
            ReminderRepository reminderRepository,
            AvailabilityService availabilityService,
            CurrentUser currentUser) {
        this.appointmentRepository = appointmentRepository;
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.reminderRepository = reminderRepository;
        this.availabilityService = availabilityService;
        this.currentUser = currentUser;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse create(AppointmentCreateRequest request, String idempotencyKey) {
        AuthenticatedUser actor = currentUser.require();
        Long userId = actor.userId();

        String key = normalizeKey(idempotencyKey);
        if (key != null) {
            var existing = appointmentRepository.findByUserIdAndIdempotencyKey(userId, key);
            if (existing.isPresent()) {
                return AppointmentMapper.toResponse(existing.get());
            }
        }

        Provider provider = providerRepository.findByIdForUpdate(request.providerId())
                .orElseThrow(() -> notFound("Provider not found"));
        if (provider.getUser().getId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.SELF_BOOKING_NOT_ALLOWED,
                    HttpStatus.CONFLICT,
                    "Providers cannot book their own services");
        }

        ProviderService service = availabilityService.requireActiveService(request.providerId(), request.serviceId());
        Instant endAt = request.startAt().plus(Duration.ofMinutes(service.getDurationMinutes()));
        availabilityService.validateBookable(provider, service, request.startAt(), endAt, null);

        User user = userRepository.getReferenceById(userId);
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setProvider(provider);
        appointment.setService(service);
        appointment.setStartAt(request.startAt());
        appointment.setEndAt(endAt);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setNotes(trimToNull(request.notes()));
        appointment.setIdempotencyKey(key);

        try {
            return AppointmentMapper.toResponse(appointmentRepository.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException ex) {
            if (key != null) {
                return appointmentRepository.findByUserIdAndIdempotencyKey(userId, key)
                        .map(AppointmentMapper::toResponse)
                        .orElseThrow(() -> conflict());
            }
            throw conflict();
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> listMine(Pageable pageable) {
        AuthenticatedUser actor = currentUser.require();
        Page<AppointmentResponse> page;
        if (actor.role() == UserRole.PROVIDER) {
            Provider provider = providerRepository.findByUserId(actor.userId())
                    .orElseThrow(() -> notFound("Provider profile not found"));
            page = appointmentRepository
                    .findByProviderIdOrderByStartAtDesc(provider.getId(), pageable)
                    .map(AppointmentMapper::toResponse);
        } else {
            page = appointmentRepository
                    .findByUserIdOrderByStartAtDesc(actor.userId(), pageable)
                    .map(AppointmentMapper::toResponse);
        }
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long appointmentId) {
        AuthenticatedUser actor = currentUser.require();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> notFound("Appointment not found"));
        if (!canAccess(appointment, actor)) {
            throw notFound("Appointment not found");
        }
        return AppointmentMapper.toResponse(appointment);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse cancel(Long appointmentId) {
        AuthenticatedUser actor = currentUser.require();
        Appointment appointment = requireForUpdate(appointmentId);
        if (!canAccess(appointment, actor)) {
            throw notFound("Appointment not found");
        }

        switch (appointment.getStatus()) {
            case CANCELLED -> throw new BusinessException(
                    ErrorCode.APPOINTMENT_ALREADY_CANCELLED, HttpStatus.CONFLICT, "Appointment is already cancelled");
            case COMPLETED -> throw new BusinessException(
                    ErrorCode.APPOINTMENT_ALREADY_COMPLETED, HttpStatus.CONFLICT, "Appointment is already completed");
            case NO_SHOW -> throw new BusinessException(
                    ErrorCode.INVALID_APPOINTMENT_STATE, HttpStatus.CONFLICT, "Appointment cannot be cancelled");
            case PENDING, CONFIRMED -> appointment.setStatus(AppointmentStatus.CANCELLED);
        }

        cancelPendingReminders(appointment.getId());
        return AppointmentMapper.toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse reschedule(Long appointmentId, AppointmentRescheduleRequest request) {
        AuthenticatedUser actor = currentUser.require();
        Appointment appointment = requireForUpdate(appointmentId);
        if (!canAccess(appointment, actor)) {
            throw notFound("Appointment not found");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException(
                    ErrorCode.INVALID_APPOINTMENT_STATE,
                    HttpStatus.CONFLICT,
                    "Only pending or confirmed appointments can be rescheduled");
        }

        Provider provider = providerRepository.findByIdForUpdate(appointment.getProvider().getId())
                .orElseThrow(() -> notFound("Provider not found"));
        ProviderService service = appointment.getService();
        Instant endAt = request.startAt().plus(Duration.ofMinutes(service.getDurationMinutes()));
        availabilityService.validateBookable(provider, service, request.startAt(), endAt, appointment.getId());

        appointment.setStartAt(request.startAt());
        appointment.setEndAt(endAt);
        if (request.notes() != null) {
            appointment.setNotes(trimToNull(request.notes()));
        }
        return AppointmentMapper.toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse confirm(Long appointmentId) {
        return transition(appointmentId, AppointmentStatus.CONFIRMED, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse complete(Long appointmentId) {
        return transition(appointmentId, AppointmentStatus.COMPLETED, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse markNoShow(Long appointmentId) {
        AppointmentResponse response = transition(appointmentId, AppointmentStatus.NO_SHOW, true);
        cancelPendingReminders(appointmentId);
        return response;
    }

    private AppointmentResponse transition(Long appointmentId, AppointmentStatus target, boolean providerOrAdmin) {
        AuthenticatedUser actor = currentUser.require();
        Appointment appointment = requireForUpdate(appointmentId);
        if (providerOrAdmin && !canManage(appointment, actor)) {
            throw notFound("Appointment not found");
        }
        if (!appointment.getStatus().canTransitionTo(target)) {
            throw new BusinessException(
                    ErrorCode.INVALID_APPOINTMENT_STATE,
                    HttpStatus.CONFLICT,
                    "Appointment cannot transition from " + appointment.getStatus() + " to " + target);
        }
        appointment.setStatus(target);
        return AppointmentMapper.toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    private static boolean canManage(Appointment appointment, AuthenticatedUser actor) {
        return actor.role() == UserRole.ADMIN
                || appointment.getProvider().getUser().getId().equals(actor.userId());
    }

    private void cancelPendingReminders(Long appointmentId) {
        List<Reminder> reminders =
                reminderRepository.findByAppointmentIdAndStatus(appointmentId, ReminderStatus.PENDING);
        for (Reminder reminder : reminders) {
            reminder.setStatus(ReminderStatus.CANCELLED);
        }
        reminderRepository.saveAll(reminders);
    }

    private Appointment requireForUpdate(Long appointmentId) {
        return appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> notFound("Appointment not found"));
    }

    private static boolean canAccess(Appointment appointment, AuthenticatedUser actor) {
        if (actor.role() == UserRole.ADMIN) {
            return true;
        }
        if (appointment.getUser().getId().equals(actor.userId())) {
            return true;
        }
        return appointment.getProvider().getUser().getId().equals(actor.userId());
    }

    private static String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    private static BusinessException conflict() {
        return new BusinessException(
                ErrorCode.APPOINTMENT_CONFLICT, HttpStatus.CONFLICT, "The selected time slot is no longer available");
    }
}
