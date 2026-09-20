package com.bookease.provider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/providers")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Providers", description = "Administrative provider approval")
public class AdminProviderController {

    private final ProviderManagementService providerManagementService;

    public AdminProviderController(ProviderManagementService providerManagementService) {
        this.providerManagementService = providerManagementService;
    }

    @GetMapping("/pending")
    @Operation(operationId = "getPendingProviders", summary = "List providers awaiting approval (ADMIN)")
    public List<ProviderResponse> pending() {
        return providerManagementService.pendingProviders();
    }

    @PostMapping("/{providerId}/approve")
    @Operation(operationId = "approveProvider", summary = "Approve a provider (ADMIN)")
    public ProviderResponse approve(@PathVariable Long providerId) {
        return providerManagementService.approve(providerId);
    }

    @PostMapping("/{providerId}/reject")
    @Operation(operationId = "rejectProvider", summary = "Reject a pending provider (ADMIN)")
    public ProviderResponse reject(
            @PathVariable Long providerId,
            @Valid @RequestBody(required = false) ProviderDecisionRequest request) {
        return providerManagementService.reject(providerId);
    }
}
