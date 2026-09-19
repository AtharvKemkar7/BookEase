package com.bookease.availability;

import com.bookease.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers/me/schedules")
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Provider Schedules", description = "Manage the authenticated provider's working schedule (UTC)")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final CurrentUser currentUser;

    public ScheduleController(ScheduleService scheduleService, CurrentUser currentUser) {
        this.scheduleService = scheduleService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createSchedule", summary = "Create a working schedule window")
    public ScheduleResponse create(@Valid @RequestBody ScheduleRequest request) {
        return scheduleService.create(currentUser.requireUserId(), request);
    }

    @GetMapping
    @Operation(operationId = "getMySchedules", summary = "List the authenticated provider's schedule")
    public List<ScheduleResponse> list() {
        return scheduleService.listMine(currentUser.requireUserId());
    }

    @PutMapping("/{scheduleId}")
    @Operation(operationId = "updateSchedule", summary = "Update a working schedule window")
    public ScheduleResponse update(@PathVariable Long scheduleId, @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.update(currentUser.requireUserId(), scheduleId, request);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deactivateSchedule", summary = "Deactivate a working schedule window")
    public void deactivate(@PathVariable Long scheduleId) {
        scheduleService.deactivate(currentUser.requireUserId(), scheduleId);
    }
}
