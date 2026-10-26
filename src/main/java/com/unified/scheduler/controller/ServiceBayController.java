package com.unified.scheduler.controller;

import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.dto.ServiceBayDto;
import com.unified.scheduler.service.ServiceBayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-bays")
@Tag(name = "Service Bays", description = "Query dealership workshop service stalls and operational bays")
public class ServiceBayController {

    private final ServiceBayService serviceBayService;

    public ServiceBayController(ServiceBayService serviceBayService) {
        this.serviceBayService = serviceBayService;
    }

    @GetMapping({"", "/search"})
    @Operation(summary = "Search operational service bays with dealership filter and Spring Data pagination for table views")
    public ResponseEntity<PageResponseDto<ServiceBayDto>> searchServiceBays(
        @Parameter(description = "Optional Dealership ID filter") @RequestParam(required = false) Long dealershipId,
        @PageableDefault(page = 0, size = 10, sort = {"bayNumber", "name"}, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(serviceBayService.searchServiceBays(dealershipId, pageable));
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup operational service bays for dropdowns with infinite-scroll pagination and quick search")
    public ResponseEntity<LookupPageResponseDto> lookupServiceBays(
        @Parameter(description = "Optional Dealership ID filter") @RequestParam(required = false) Long dealershipId,
        @Parameter(description = "Keyword search for bay name or number") @RequestParam(required = false) String query,
        @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size limit (1-100, default 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(serviceBayService.lookupServiceBays(dealershipId, query, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service bay by ID")
    public ResponseEntity<ServiceBayDto> getServiceBayById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceBayService.getServiceBayById(id));
    }
}
