package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload to reassign an appointment to another qualified technician")
public record ReassignTechnicianRequestDto(
    @NotNull(message = "Technician ID is required")
    @Schema(description = "ID of the target qualified technician", example = "2")
    Long technicianId,

    @Schema(description = "Optional reason for reassigning technician", example = "Technician on sick leave")
    String reason
) {}
