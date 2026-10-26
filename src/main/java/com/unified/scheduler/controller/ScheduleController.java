package com.unified.scheduler.controller;

import com.unified.scheduler.dto.AvailabilityScheduleResponseDto;
import com.unified.scheduler.service.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Schedules & Availability", description = "Query dynamic bay and technician availability across time slots")
public class ScheduleController {

    private final SchedulingService schedulingService;

    public ScheduleController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @GetMapping("/available-slots")
    @Operation(summary = "Query available appointment slots structured for calendar UI rendering with flexible date range (day/week/month) and optional desired time")
    public ResponseEntity<AvailabilityScheduleResponseDto> getAvailableSlots(
        @Parameter(description = "ID of dealership", required = true)
        @RequestParam Long dealershipId,

        @Parameter(description = "ID of service type", required = true)
        @RequestParam Long serviceTypeId,

        @Parameter(description = "Start date (YYYY-MM-DD). Defaults to today if omitted")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

        @Parameter(description = "End date (YYYY-MM-DD). Defaults to 1 week from startDate if omitted. Maximum range is 31 days (1 month)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

        @Parameter(description = "Target single schedule date (YYYY-MM-DD) for single-day query compatibility")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

        @Parameter(description = "Specific desired start time (HH:mm:ss or HH:mm) to filter slots across the date range")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime desiredTime
    ) {
        LocalDate effectiveStart = startDate != null ? startDate : date;
        LocalDate effectiveEnd = endDate != null ? endDate : (date != null ? date : null);

        AvailabilityScheduleResponseDto schedule = schedulingService.getAvailableSlots(
            dealershipId, serviceTypeId, effectiveStart, effectiveEnd, desiredTime
        );
        return ResponseEntity.ok(schedule);
    }
}
