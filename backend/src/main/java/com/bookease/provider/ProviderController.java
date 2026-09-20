package com.bookease.provider;

import com.bookease.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Providers", description = "Provider onboarding and self-management")
public class ProviderController {

    private final ProviderManagementService providerManagementService;
    private final CurrentUser currentUser;

    public ProviderController(ProviderManagementService providerManagementService, CurrentUser currentUser) {
        this.providerManagementService = providerManagementService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "createProviderProfile",
            summary = "Create a provider profile",
            description = "USER submits a provider application in PENDING status. The account stays USER until an "
                    + "admin approves it, which promotes the account to PROVIDER.")
    public ProviderResponse onboard(@Valid @RequestBody ProviderCreateRequest request) {
        return providerManagementService.onboard(currentUser.requireUserId(), request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'PROVIDER')")
    @Operation(
            operationId = "getMyProviderProfile",
            summary = "Get the authenticated user's provider profile or application")
    public ProviderResponse getMine() {
        return providerManagementService.getMine(currentUser.requireUserId());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'PROVIDER')")
    @Operation(operationId = "updateMyProviderProfile", summary = "Update the authenticated provider profile")
    public ProviderResponse updateMine(@Valid @RequestBody ProviderUpdateRequest request) {
        return providerManagementService.updateMine(currentUser.requireUserId(), request);
    }
}
