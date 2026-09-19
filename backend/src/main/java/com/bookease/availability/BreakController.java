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
@RequestMapping("/api/v1/providers/me/breaks")
@PreAuthorize("hasRole('PROVIDER')")
@Tag(name = "Provider Breaks", description = "Manage the authenticated provider's breaks (UTC)")
public class BreakController {

    private final BreakService breakService;
    private final CurrentUser currentUser;

    public BreakController(BreakService breakService, CurrentUser currentUser) {
        this.breakService = breakService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createBreak", summary = "Create a break window")
    public BreakResponse create(@Valid @RequestBody BreakRequest request) {
        return breakService.create(currentUser.requireUserId(), request);
    }

    @GetMapping
    @Operation(operationId = "getMyBreaks", summary = "List the authenticated provider's breaks")
    public List<BreakResponse> list() {
        return breakService.listMine(currentUser.requireUserId());
    }

    @PutMapping("/{breakId}")
    @Operation(operationId = "updateBreak", summary = "Update a break window")
    public BreakResponse update(@PathVariable Long breakId, @Valid @RequestBody BreakRequest request) {
        return breakService.update(currentUser.requireUserId(), breakId, request);
    }

    @DeleteMapping("/{breakId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deactivateBreak", summary = "Deactivate a break window")
    public void deactivate(@PathVariable Long breakId) {
        breakService.deactivate(currentUser.requireUserId(), breakId);
    }
}
