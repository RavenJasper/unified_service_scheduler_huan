package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Calendar day availability group containing available slots")
public record DailyAvailabilityDto(
    @Schema(description = "Calendar date (YYYY-MM-DD)", example = "2026-11-01")
    LocalDate date,

    @Schema(description = "Day of week", example = "SUNDAY")
    DayOfWeek dayOfWeek,

    @Schema(description = "Number of available slots on this day", example = "8")
    int availableSlotCount,

    @Schema(description = "List of available booking slots for this day")
    List<AvailableSlotDto> slots
) {}
