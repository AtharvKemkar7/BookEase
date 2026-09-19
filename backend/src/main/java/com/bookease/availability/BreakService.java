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
public class BreakService {

    private final BreakRepository breakRepository;
    private final WorkingScheduleRepository workingScheduleRepository;
    private final ProviderManagementService providerManagementService;

    public BreakService(
            BreakRepository breakRepository,
            WorkingScheduleRepository workingScheduleRepository,
            ProviderManagementService providerManagementService) {
        this.breakRepository = breakRepository;
        this.workingScheduleRepository = workingScheduleRepository;
        this.providerManagementService = providerManagementService;
    }

    @Transactional
    public BreakResponse create(Long userId, BreakRequest request) {
        Provider provider = providerManagementService.requireByUser(userId);
        validateRange(request);
        ensureWithinSchedule(provider.getId(), request);
        ensureNoOverlap(provider.getId(), request, null);
        Break brk = new Break();
        brk.setProvider(provider);
        apply(brk, request);
        return toResponse(breakRepository.saveAndFlush(brk));
    }

    @Transactional(readOnly = true)
    public List<BreakResponse> listMine(Long userId) {
        Provider provider = providerManagementService.requireByUser(userId);
        return breakRepository.findByProviderIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(provider.getId()).stream()
                .map(BreakService::toResponse)
                .toList();
    }

    @Transactional
    public BreakResponse update(Long userId, Long breakId, BreakRequest request) {
        Provider provider = providerManagementService.requireByUser(userId);
        Break brk = requireOwned(provider.getId(), breakId);
        validateRange(request);
        ensureWithinSchedule(provider.getId(), request);
        ensureNoOverlap(provider.getId(), request, breakId);
        apply(brk, request);
        return toResponse(breakRepository.saveAndFlush(brk));
    }

    @Transactional
    public void deactivate(Long userId, Long breakId) {
        Provider provider = providerManagementService.requireByUser(userId);
        Break brk = requireOwned(provider.getId(), breakId);
        brk.setActive(false);
        breakRepository.saveAndFlush(brk);
    }

    private Break requireOwned(Long providerId, Long breakId) {
        return breakRepository.findByIdAndProviderId(breakId, providerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Break not found"));
    }

    private void ensureWithinSchedule(Long providerId, BreakRequest request) {
        boolean within = workingScheduleRepository
                .findByProviderIdAndDayOfWeekAndActiveTrue(providerId, request.dayOfWeek()).stream()
                .anyMatch(window -> TimeRanges.contains(
                        window.getStartTime(), window.getEndTime(), request.startTime(), request.endTime()));
        if (!within) {
            throw new BusinessException(
                    ErrorCode.BREAK_OUTSIDE_SCHEDULE,
                    HttpStatus.CONFLICT,
                    "Break must be fully inside an active working schedule");
        }
    }

    private void ensureNoOverlap(Long providerId, BreakRequest request, Long excludeId) {
        boolean overlapping = breakRepository
                .findByProviderIdAndDayOfWeekAndActiveTrue(providerId, request.dayOfWeek()).stream()
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .anyMatch(existing -> TimeRanges.overlaps(
                        request.startTime(), request.endTime(), existing.getStartTime(), existing.getEndTime()));
        if (overlapping) {
            throw new BusinessException(
                    ErrorCode.BREAK_OVERLAP, HttpStatus.CONFLICT, "Break overlaps an existing break for this day");
        }
    }

    private static void validateRange(BreakRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(
                    ErrorCode.INVALID_BREAK, HttpStatus.BAD_REQUEST, "Break start time must be before end time");
        }
    }

    private static void apply(Break brk, BreakRequest request) {
        brk.setDayOfWeek(request.dayOfWeek());
        brk.setStartTime(request.startTime());
        brk.setEndTime(request.endTime());
        if (request.active() != null) {
            brk.setActive(request.active());
        }
    }

    private static BreakResponse toResponse(Break brk) {
        return new BreakResponse(
                brk.getId(),
                brk.getProvider().getId(),
                brk.getDayOfWeek(),
                brk.getStartTime(),
                brk.getEndTime(),
                brk.isActive());
    }
}
