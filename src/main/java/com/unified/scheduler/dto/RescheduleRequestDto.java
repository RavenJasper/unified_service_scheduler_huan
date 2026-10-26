package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Schema(description = "Request payload to reschedule an existing confirmed appointment")
public record RescheduleRequestDto(
    @NotNull(message = "New start time is required")
    @Future(message = "New start time must be in the future")
    @Schema(description = "Desired new appointment start time with UTC offset", example = "2026-11-02T10:00:00Z")
    OffsetDateTime newStartTime,

    @Schema(description = "Optional reason for rescheduling", example = "Customer requested later time")
    String reason,

    @Schema(description = "Optional preferred technician ID for the rescheduled slot", example = "2")
    Long technicianId
) {
    public RescheduleRequestDto(OffsetDateTime newStartTime, String reason) {
        this(newStartTime, reason, null);
    }
}
