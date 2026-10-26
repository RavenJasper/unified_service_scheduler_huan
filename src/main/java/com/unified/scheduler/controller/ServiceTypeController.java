package com.unified.scheduler.controller;

import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.dto.ServiceTypeDto;
import com.unified.scheduler.service.ServiceTypeService;
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
@RequestMapping("/api/v1/service-types")
@Tag(name = "Service Types", description = "Query automotive maintenance and service catalogue items")
public class ServiceTypeController {

    private final ServiceTypeService serviceTypeService;

    public ServiceTypeController(ServiceTypeService serviceTypeService) {
        this.serviceTypeService = serviceTypeService;
    }

    @GetMapping({"", "/search"})
    @Operation(summary = "Search active service types with keyword filtering and Spring Data pagination for table views")
    public ResponseEntity<PageResponseDto<ServiceTypeDto>> searchServiceTypes(
        @Parameter(description = "Keyword search for code or name") @RequestParam(required = false) String query,
        @Parameter(description = "Filter by required skill code") @RequestParam(required = false) String skillCode,
        @PageableDefault(page = 0, size = 10, sort = {"code", "name"}, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(serviceTypeService.searchServiceTypes(query, skillCode, pageable));
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup active service types for dropdowns with infinite-scroll pagination and quick search")
    public ResponseEntity<LookupPageResponseDto> lookupServiceTypes(
        @Parameter(description = "Keyword search for code or name") @RequestParam(required = false) String query,
        @Parameter(description = "Filter by required skill code") @RequestParam(required = false) String skillCode,
        @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size limit (1-100, default 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(serviceTypeService.lookupServiceTypes(query, skillCode, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service type by ID")
    public ResponseEntity<ServiceTypeDto> getServiceTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceTypeService.getServiceTypeById(id));
    }
}
