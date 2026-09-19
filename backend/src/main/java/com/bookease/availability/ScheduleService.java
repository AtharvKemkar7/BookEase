package com.bookease.availability;

import com.bookease.common.exception.BusinessException;
import com.bookease.common.exception.ErrorCode;
import com.bookease.provider.Provider;
import com.bookease.provider.ProviderManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScheduleService {

    private final WorkingScheduleRepository workingScheduleRepository;
    private final ProviderManagementService providerManagementService;

    public ScheduleService(
            WorkingScheduleRepository workingScheduleRepository,
            ProviderManagementService providerManagementService) {
        this.workingScheduleRepository = workingScheduleRepository;
        this.providerManagementService = providerManagementService;
    }

    @Transactional
    public ScheduleResponse create(Long userId, ScheduleRequest request) {
        Provider provider = providerManagementService.requireByUser(userId);
        validateRange(request);
        ensureNoOverlap(provider.getId(), request, null);
        WorkingSchedule schedule = new WorkingSchedule();
        schedule.setProvider(provider);
        apply(schedule, request);
        return toResponse(workingScheduleRepository.saveAndFlush(schedule));
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listMine(Long userId) {
        Provider provider = providerManagementService.requireByUser(userId);
        return workingScheduleRepository
                .findByProviderIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(provider.getId()).stream()
                .map(ScheduleService::toResponse)
                .toList();
    }

    @Transactional
    public ScheduleResponse update(Long userId, Long scheduleId, ScheduleRequest request) {
        Provider provider = providerManagementService.requireByUser(userId);
        WorkingSchedule schedule = requireOwned(provider.getId(), scheduleId);
        validateRange(request);
        ensureNoOverlap(provider.getId(), request, scheduleId);
        apply(schedule, request);
        return toResponse(workingScheduleRepository.saveAndFlush(schedule));
    }

    @Transactional
    public void deactivate(Long userId, Long scheduleId) {
        Provider provider = providerManagementService.requireByUser(userId);
        WorkingSchedule schedule = requireOwned(provider.getId(), scheduleId);
        schedule.setActive(false);
        workingScheduleRepository.saveAndFlush(schedule);
    }

    private WorkingSchedule requireOwned(Long providerId, Long scheduleId) {
        return workingScheduleRepository.findByIdAndProviderId(scheduleId, providerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Schedule not found"));
    }

    private void ensureNoOverlap(Long providerId, ScheduleRequest request, Long excludeId) {
        boolean overlapping = workingScheduleRepository
                .findByProviderIdAndDayOfWeekAndActiveTrue(providerId, request.dayOfWeek()).stream()
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .anyMatch(existing -> TimeRanges.overlaps(
                        request.startTime(), request.endTime(), existing.getStartTime(), existing.getEndTime()));
        if (overlapping) {
            throw new BusinessException(
                    ErrorCode.SCHEDULE_OVERLAP,
                    HttpStatus.CONFLICT,
                    "Schedule overlaps an existing schedule for this day");
        }
    }

    private static void validateRange(ScheduleRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(
                    ErrorCode.INVALID_SCHEDULE,
                    HttpStatus.BAD_REQUEST,
                    "Schedule start time must be before end time");
        }
    }

    private static void apply(WorkingSchedule schedule, ScheduleRequest request) {
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        if (request.active() != null) {
            schedule.setActive(request.active());
        }
    }

    private static ScheduleResponse toResponse(WorkingSchedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getProvider().getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isActive());
    }
}
