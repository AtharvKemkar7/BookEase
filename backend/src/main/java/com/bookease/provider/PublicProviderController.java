package com.bookease.provider;

import com.bookease.common.response.PageResponse;
import com.bookease.service.ProviderServiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Provider Discovery", description = "Public discovery of approved providers")
public class PublicProviderController {

    private final ProviderManagementService providerManagementService;

    public PublicProviderController(ProviderManagementService providerManagementService) {
        this.providerManagementService = providerManagementService;
    }

    @GetMapping
    @Operation(
            operationId = "searchProviders",
            summary = "Search approved providers",
            description = "Returns only APPROVED providers. Supports filtering by category, city and name, plus "
                    + "pagination and sorting.")
    public PageResponse<ProviderSummaryResponse> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "businessName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Page<ProviderSummaryResponse> results =
                providerManagementService.searchApproved(categoryId, city, name, PageRequest.of(page, safeSize, sort));
        return PageResponse.from(results);
    }

    @GetMapping("/{providerId}")
    @Operation(operationId = "getProviderDetails", summary = "Get an approved provider")
    public ProviderSummaryResponse get(@PathVariable Long providerId) {
        return providerManagementService.getApproved(providerId);
    }

    @GetMapping("/{providerId}/services")
    @Operation(operationId = "getProviderServices", summary = "List active services of an approved provider")
    public List<ProviderServiceResponse> services(@PathVariable Long providerId) {
        return providerManagementService.listApprovedServices(providerId);
    }
}
