package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Availability schedule tailored for frontend calendar rendering")
public record AvailabilityScheduleResponseDto(
    @Schema(description = "Dealership ID", example = "1")
    Long dealershipId,

    @Schema(description = "Dealership name", example = "Central Metro Dealership")
    String dealershipName,

    @Schema(description = "Dealership operating timezone", example = "UTC")
    String timeZone,

    @Schema(description = "Service type ID", example = "2")
    Long serviceTypeId,

    @Schema(description = "Service type name", example = "Comprehensive Brake System Service")
    String serviceTypeName,

    @Schema(description = "Standard service duration in minutes", example = "60")
    int serviceDurationMinutes,

    @Schema(description = "Start date of queried schedule window", example = "2026-11-01")
    LocalDate startDate,

    @Schema(description = "End date of queried schedule window", example = "2026-11-07")
    LocalDate endDate,

    @Schema(description = "Total number of available slots across all days in window", example = "42")
    int totalAvailableSlots,

    @Schema(description = "Available slots grouped by calendar day")
    List<DailyAvailabilityDto> days
) {}
