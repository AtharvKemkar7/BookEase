package com.bookease.availability;

import com.bookease.appointment.Appointment;
import com.bookease.appointment.AppointmentRepository;
import com.bookease.common.exception.BusinessException;
import com.bookease.common.exception.ErrorCode;
import com.bookease.provider.Provider;
import com.bookease.provider.ProviderManagementService;
import com.bookease.service.ProviderService;
import com.bookease.service.ProviderServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {

    private static final int SLOT_STEP_MINUTES = 15;

    private final WorkingScheduleRepository workingScheduleRepository;
    private final BreakRepository breakRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderManagementService providerManagementService;

    public AvailabilityService(
            WorkingScheduleRepository workingScheduleRepository,
            BreakRepository breakRepository,
            AppointmentRepository appointmentRepository,
            ProviderServiceRepository providerServiceRepository,
            ProviderManagementService providerManagementService) {
        this.workingScheduleRepository = workingScheduleRepository;
        this.breakRepository = breakRepository;
        this.appointmentRepository = appointmentRepository;
        this.providerServiceRepository = providerServiceRepository;
        this.providerManagementService = providerManagementService;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(Long providerId, Long serviceId, LocalDate date) {
        Provider provider = providerManagementService.requireApproved(providerId);
        ProviderService service = requireActiveService(providerId, serviceId);

        List<AvailabilitySlot> slots = new ArrayList<>();
        if (date != null) {
            slots = computeSlots(provider, service, date);
        }
        return new AvailabilityResponse(providerId, serviceId, date, service.getDurationMinutes(), slots);
    }

    @Transactional(readOnly = true)
    public void validateBookable(
            Provider provider, ProviderService service, Instant startAt, Instant endAt, Long excludeAppointmentId) {
        if (!provider.isApproved()) {
            throw new BusinessException(
                    ErrorCode.PROVIDER_NOT_AVAILABLE, HttpStatus.CONFLICT, "Provider is not available for booking");
        }
        if (service.getProvider() == null || !service.getProvider().getId().equals(provider.getId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_PROVIDER_SERVICE, HttpStatus.CONFLICT, "Service does not belong to provider");
        }
        if (!service.isActive()) {
            throw new BusinessException(
                    ErrorCode.SERVICE_NOT_AVAILABLE, HttpStatus.CONFLICT, "Service is not available");
        }
        if (!startAt.isBefore(endAt)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "startAt must be before endAt");
        }
        if (startAt.isBefore(Instant.now())) {
            throw new BusinessException(
                    ErrorCode.APPOINTMENT_IN_PAST, HttpStatus.CONFLICT, "Appointment start time is in the past");
        }

        LocalDateTime localStart = LocalDateTime.ofInstant(startAt, ZoneOffset.UTC);
        LocalDate date = localStart.toLocalDate();

        boolean withinWindow = workingScheduleRepository
                .findByProviderIdAndDayOfWeekAndActiveTrue(provider.getId(), date.getDayOfWeek()).stream()
                .anyMatch(window -> {
                    Instant windowStart = toInstant(date, window.getStartTime());
                    Instant windowEnd = toInstant(date, window.getEndTime());
                    return !startAt.isBefore(windowStart) && !endAt.isAfter(windowEnd);
                });
        if (!withinWindow) {
            throw new BusinessException(
                    ErrorCode.OUTSIDE_WORKING_HOURS,
                    HttpStatus.CONFLICT,
                    "Appointment is outside the provider working hours");
        }

        LocalTime localStartTime = localStart.toLocalTime();
        LocalTime localEndTime = LocalDateTime.ofInstant(endAt, ZoneOffset.UTC).toLocalTime();
        boolean duringBreak = breakRepository
                .findByProviderIdAndDayOfWeekAndActiveTrue(provider.getId(), date.getDayOfWeek()).stream()
                .anyMatch(brk -> localStartTime.isBefore(brk.getEndTime()) && localEndTime.isAfter(brk.getStartTime()));
        if (duringBreak) {
            throw new BusinessException(
                    ErrorCode.DURING_PROVIDER_BREAK, HttpStatus.CONFLICT, "Appointment overlaps a provider break");
        }

        long overlaps = appointmentRepository.countOverlapping(provider.getId(), startAt, endAt, excludeAppointmentId);
        if (overlaps > 0) {
            throw new BusinessException(
                    ErrorCode.APPOINTMENT_CONFLICT, HttpStatus.CONFLICT, "The selected time slot is no longer available");
        }
    }

    @Transactional(readOnly = true)
    public ProviderService requireActiveService(Long providerId, Long serviceId) {
        ProviderService service = providerServiceRepository.findByIdAndProviderId(serviceId, providerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Service not found"));
        if (!service.isActive()) {
            throw new BusinessException(
                    ErrorCode.SERVICE_NOT_AVAILABLE, HttpStatus.CONFLICT, "Service is not available");
        }
        return service;
    }

    private List<AvailabilitySlot> computeSlots(Provider provider, ProviderService service, LocalDate date) {
        List<WorkingSchedule> windows = workingScheduleRepository
                .findByProviderIdAndDayOfWeekAndActiveTrue(provider.getId(), date.getDayOfWeek());
        if (windows.isEmpty()) {
            return List.of();
        }
        List<Break> breaks = breakRepository
                .findByProviderIdAndDayOfWeekAndActiveTrue(provider.getId(), date.getDayOfWeek());

        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plus(Duration.ofDays(1));
        List<Appointment> occupied = appointmentRepository.findOccupied(provider.getId(), dayStart, dayEnd);

        Duration duration = Duration.ofMinutes(service.getDurationMinutes());
        Duration step = Duration.ofMinutes(SLOT_STEP_MINUTES);
        Instant now = Instant.now();
        List<AvailabilitySlot> slots = new ArrayList<>();

        for (WorkingSchedule window : windows) {
            Instant windowStart = toInstant(date, window.getStartTime());
            Instant windowEnd = toInstant(date, window.getEndTime());
            Instant slotStart = windowStart;
            while (!slotStart.plus(duration).isAfter(windowEnd)) {
                Instant slotEnd = slotStart.plus(duration);
                if (!slotStart.isBefore(now) && isFree(slotStart, slotEnd, date, breaks, occupied)) {
                    slots.add(new AvailabilitySlot(slotStart, slotEnd));
                }
                slotStart = slotStart.plus(step);
            }
        }
        return slots;
    }

    private static boolean isFree(
            Instant slotStart, Instant slotEnd, LocalDate date, List<Break> breaks, List<Appointment> occupied) {
        LocalTime localStart = LocalDateTime.ofInstant(slotStart, ZoneOffset.UTC).toLocalTime();
        LocalTime localEnd = LocalDateTime.ofInstant(slotEnd, ZoneOffset.UTC).toLocalTime();
        for (Break brk : breaks) {
            if (localStart.isBefore(brk.getEndTime()) && localEnd.isAfter(brk.getStartTime())) {
                return false;
            }
        }
        for (Appointment appointment : occupied) {
            if (slotStart.isBefore(appointment.getEndAt()) && slotEnd.isAfter(appointment.getStartAt())) {
                return false;
            }
        }
        return true;
    }

    private static Instant toInstant(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time).toInstant(ZoneOffset.UTC);
    }
}
