package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Technician available to perform the requested service in a specific slot")
public record AvailableTechnicianDto(
    @Schema(description = "Technician ID", example = "1")
    Long id,

    @Schema(description = "Employee Code", example = "TECH-001")
    String employeeCode,

    @Schema(description = "Full Name", example = "Bob Vance")
    String name
) {}
