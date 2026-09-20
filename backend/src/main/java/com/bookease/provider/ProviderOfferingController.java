package com.bookease.provider;

import com.bookease.security.CurrentUser;
import com.bookease.service.ProviderServiceRequest;
import com.bookease.service.ProviderServiceResponse;
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
@RequestMapping("/api/v1/providers/me/services")
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Provider Services", description = "Manage the authenticated provider's service offerings")
public class ProviderOfferingController {

    private final ProviderOfferingService providerOfferingService;
    private final CurrentUser currentUser;

    public ProviderOfferingController(ProviderOfferingService providerOfferingService, CurrentUser currentUser) {
        this.providerOfferingService = providerOfferingService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createProviderService", summary = "Create a service offering")
    public ProviderServiceResponse create(@Valid @RequestBody ProviderServiceRequest request) {
        return providerOfferingService.create(currentUser.requireUserId(), request);
    }

    @GetMapping
    @Operation(operationId = "getMyProviderServices", summary = "List the authenticated provider's services")
    public List<ProviderServiceResponse> list() {
        return providerOfferingService.listMine(currentUser.requireUserId());
    }

    @PutMapping("/{serviceId}")
    @Operation(operationId = "updateProviderService", summary = "Update a service offering")
    public ProviderServiceResponse update(
            @PathVariable Long serviceId, @Valid @RequestBody ProviderServiceRequest request) {
        return providerOfferingService.update(currentUser.requireUserId(), serviceId, request);
    }

    @DeleteMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            operationId = "deactivateProviderService",
            summary = "Deactivate a service offering",
            description = "Soft-deletes the service so existing appointment history remains valid.")
    public void deactivate(@PathVariable Long serviceId) {
        providerOfferingService.deactivate(currentUser.requireUserId(), serviceId);
    }
}
