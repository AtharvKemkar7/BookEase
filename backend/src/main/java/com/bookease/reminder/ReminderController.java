package com.bookease.reminder;

import com.bookease.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reminders")
@Tag(name = "Reminders", description = "Manage reminders for the authenticated user's appointments")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "createReminder",
            summary = "Create a reminder",
            description = "The appointment must belong to the caller. Reminder time must be in the future and "
                    + "before the appointment. Duplicates are rejected.")
    public ReminderResponse create(@Valid @RequestBody ReminderCreateRequest request) {
        return reminderService.create(request);
    }

    @GetMapping("/me")
    @Operation(operationId = "getMyReminders", summary = "List the authenticated user's reminders")
    public PageResponse<ReminderResponse> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return reminderService.listMine(PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "reminderAt")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "cancelReminder", summary = "Cancel a pending reminder")
    public void cancel(@PathVariable Long id) {
        reminderService.cancel(id);
    }
}
