package com.unified.scheduler.controller;

import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.dto.TechnicianDto;
import com.unified.scheduler.service.TechnicianService;
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
@RequestMapping("/api/v1/technicians")
@Tag(name = "Technicians", description = "Query automotive technicians and their certified skill sets")
public class TechnicianController {

    private final TechnicianService technicianService;

    public TechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    @GetMapping({"", "/search"})
    @Operation(summary = "Search technicians with keyword, dealership, skill filters and Spring Data pagination for table views")
    public ResponseEntity<PageResponseDto<TechnicianDto>> searchTechnicians(
        @Parameter(description = "Keyword search for employee code, name, or email") @RequestParam(required = false) String query,
        @Parameter(description = "Optional Dealership ID filter") @RequestParam(required = false) Long dealershipId,
        @Parameter(description = "Optional Skill Code filter") @RequestParam(required = false) String skillCode,
        @PageableDefault(page = 0, size = 10, sort = {"employeeCode", "lastName"}, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(technicianService.searchTechnicians(query, dealershipId, skillCode, pageable));
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup active technicians for dropdowns with infinite-scroll pagination and quick search")
    public ResponseEntity<LookupPageResponseDto> lookupTechnicians(
        @Parameter(description = "Optional Dealership ID filter") @RequestParam(required = false) Long dealershipId,
        @Parameter(description = "Keyword search for employee code, name, or email") @RequestParam(required = false) String query,
        @Parameter(description = "Optional Skill Code filter") @RequestParam(required = false) String skillCode,
        @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size limit (1-100, default 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(technicianService.lookupTechnicians(dealershipId, query, skillCode, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get technician by ID along with their certified skill sets")
    public ResponseEntity<TechnicianDto> getTechnicianById(@PathVariable Long id) {
        return ResponseEntity.ok(technicianService.getTechnicianById(id));
    }
}
