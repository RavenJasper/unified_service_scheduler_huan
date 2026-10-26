package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Service bay / lift stall available for an appointment in a specific slot")
public record AvailableBayDto(
    @Schema(description = "Service Bay ID", example = "1")
    Long id,

    @Schema(description = "Bay Number", example = "1")
    int bayNumber,

    @Schema(description = "Bay Name / Description", example = "Main Hydraulic Lift 1")
    String name
) {}
