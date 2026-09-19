package com.bookease.appointment;

import com.bookease.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Booking operations. Ownership is derived from the JWT.")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "createAppointment",
            summary = "Create an appointment",
            description = "Books an active service with an approved provider. Ownership comes from the JWT. The "
                    + "interval is fully revalidated inside the transaction; conflicting bookings return 409. An "
                    + "optional Idempotency-Key header prevents duplicate creation on retries.")
    public AppointmentResponse create(
            @Valid @RequestBody AppointmentCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return appointmentService.create(request, idempotencyKey);
    }

    @GetMapping("/me")
    @Operation(operationId = "getMyAppointments", summary = "List the authenticated user's appointments")
    public PageResponse<AppointmentResponse> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return appointmentService.listMine(PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "startAt")));
    }

    @GetMapping("/{appointmentId}")
    @Operation(
            operationId = "getAppointment",
            summary = "Get an appointment",
            description = "Accessible to the owning user, the owning provider, or an admin. Unknown or "
                    + "unauthorized ids return 404 to avoid resource enumeration.")
    public AppointmentResponse get(@PathVariable Long appointmentId) {
        return appointmentService.get(appointmentId);
    }

    @PostMapping("/{appointmentId}/cancel")
    @Operation(operationId = "cancelAppointment", summary = "Cancel an appointment")
    public AppointmentResponse cancel(@PathVariable Long appointmentId) {
        return appointmentService.cancel(appointmentId);
    }

    @PostMapping("/{appointmentId}/reschedule")
    @Operation(
            operationId = "rescheduleAppointment",
            summary = "Reschedule an appointment",
            description = "Revalidates provider status, service status, working hours, breaks and overlap. The "
                    + "appointment does not conflict with itself.")
    public AppointmentResponse reschedule(
            @PathVariable Long appointmentId,
            @Valid @RequestBody AppointmentRescheduleRequest request) {
        return appointmentService.reschedule(appointmentId, request);
    }

    @PostMapping("/{appointmentId}/confirm")
    @Operation(
            operationId = "confirmAppointment",
            summary = "Confirm a pending appointment",
            description = "Provider or admin only. PENDING to CONFIRMED.")
    public AppointmentResponse confirm(@PathVariable Long appointmentId) {
        return appointmentService.confirm(appointmentId);
    }

    @PostMapping("/{appointmentId}/complete")
    @Operation(
            operationId = "completeAppointment",
            summary = "Mark an appointment completed",
            description = "Provider or admin only. CONFIRMED to COMPLETED.")
    public AppointmentResponse complete(@PathVariable Long appointmentId) {
        return appointmentService.complete(appointmentId);
    }

    @PostMapping("/{appointmentId}/no-show")
    @Operation(
            operationId = "markAppointmentNoShow",
            summary = "Mark an appointment as no-show",
            description = "Provider or admin only. PENDING or CONFIRMED to NO_SHOW. Releases the slot.")
    public AppointmentResponse noShow(@PathVariable Long appointmentId) {
        return appointmentService.markNoShow(appointmentId);
    }
}
