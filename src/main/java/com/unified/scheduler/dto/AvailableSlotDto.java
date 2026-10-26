package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Individual available time slot for appointment booking")
public record AvailableSlotDto(
    @Schema(description = "Slot start timestamp with UTC offset", example = "2026-11-01T08:00:00Z")
    OffsetDateTime startTime,

    @Schema(description = "Slot end timestamp with UTC offset", example = "2026-11-01T09:00:00Z")
    OffsetDateTime endTime,

    @Schema(description = "Human-readable time range string for easy calendar UI rendering", example = "08:00 - 09:00")
    String formattedTime,

    @Schema(description = "Available booking capacity in this slot (concurrent vehicles that can be serviced)", example = "2")
    int availableCapacity,

    @Schema(description = "List of qualified technicians available for this slot (supports UI hover tooltips or preferred tech selection)")
    List<AvailableTechnicianDto> availableTechnicians,

    @Schema(description = "List of operational service bays available for this slot (supports workshop management & dispatching)")
    List<AvailableBayDto> availableBays
) {}
