package com.unified.scheduler.controller;

import com.unified.scheduler.dto.DealershipDto;
import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.service.DealershipService;
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
@RequestMapping("/api/v1/dealerships")
@Tag(name = "Dealerships", description = "Query dealership catalogues, locations, and operating hours")
public class DealershipController {

    private final DealershipService dealershipService;

    public DealershipController(DealershipService dealershipService) {
        this.dealershipService = dealershipService;
    }

    @GetMapping({"", "/search"})
    @Operation(summary = "Search active dealerships with keyword filtering and Spring Data pagination for table views")
    public ResponseEntity<PageResponseDto<DealershipDto>> searchDealerships(
        @Parameter(description = "Keyword search for code or name") @RequestParam(required = false) String query,
        @Parameter(description = "Filter by city") @RequestParam(required = false) String city,
        @PageableDefault(page = 0, size = 10, sort = {"code", "name"}, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(dealershipService.searchDealerships(query, city, pageable));
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup active dealerships for dropdowns with infinite-scroll pagination and quick search")
    public ResponseEntity<LookupPageResponseDto> lookupDealerships(
        @Parameter(description = "Keyword search for code or name") @RequestParam(required = false) String query,
        @Parameter(description = "Filter by city") @RequestParam(required = false) String city,
        @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size limit (1-100, default 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(dealershipService.lookupDealerships(query, city, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dealership by ID")
    public ResponseEntity<DealershipDto> getDealershipById(@PathVariable Long id) {
        return ResponseEntity.ok(dealershipService.getDealershipById(id));
    }
}
