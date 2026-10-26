package com.unified.scheduler.controller;

import com.unified.scheduler.dto.CustomerDto;
import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Query customer master data and customer accounts")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping({"", "/search"})
    @Operation(summary = "Search active customers with keyword filtering and Spring Data pagination for table views")
    public ResponseEntity<PageResponseDto<CustomerDto>> searchCustomers(
        @Parameter(description = "Keyword search for code, first name, last name, email, or phone") @RequestParam(required = false) String query,
        @PageableDefault(page = 0, size = 10, sort = {"customerCode", "lastName", "firstName"}, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(customerService.searchCustomers(query, pageable));
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup active customers for dropdowns with infinite-scroll pagination and quick search")
    public ResponseEntity<LookupPageResponseDto> lookupCustomers(
        @Parameter(description = "Keyword search for code, name, email, phone") @RequestParam(required = false) String query,
        @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size limit (1-100, default 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(customerService.lookupCustomers(query, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer profile by unique ID")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/code/{customerCode}")
    @Operation(summary = "Get customer profile by unique natural business code (e.g. CUST-001)")
    public ResponseEntity<CustomerDto> getCustomerByCode(@PathVariable String customerCode) {
        return ResponseEntity.ok(customerService.getCustomerByCode(customerCode));
    }
}
