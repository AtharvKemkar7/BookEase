package com.bookease.availability;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Availability", description = "Public availability lookup (UTC)")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/{providerId}/availability")
    @Operation(
            operationId = "checkAvailability",
            summary = "Check available booking slots",
            description = "Returns bookable UTC slots for an approved provider and active service on a date. "
                    + "Accounts for working schedule, breaks, existing appointments and the current time. "
                    + "The same validation runs again during booking.")
    public AvailabilityResponse availability(
            @PathVariable Long providerId,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availabilityService.availability(providerId, serviceId, date);
    }
}
