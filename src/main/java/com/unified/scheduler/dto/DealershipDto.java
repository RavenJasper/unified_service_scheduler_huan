package com.unified.scheduler.dto;

import com.unified.scheduler.domain.Dealership;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

@Schema(description = "Dealership profile and operating schedule")
public record DealershipDto(
    @Schema(description = "Numeric identifier of dealership", example = "1")
    Long id,

    @Schema(description = "Business code of dealership", example = "DLR-METRO")
    String code,

    @Schema(description = "Official trade name", example = "Central Metro Dealership")
    String name,

    @Schema(description = "Street address of dealership branch", example = "100 Main Boulevard")
    String address,

    @Schema(description = "Municipality / City", example = "Automotive City")
    String city,

    @Schema(description = "Opening time in local branch hours", example = "08:00:00")
    LocalTime operatingOpenTime,

    @Schema(description = "Closing time in local branch hours", example = "18:00:00")
    LocalTime operatingCloseTime,

    @Schema(description = "IANA timezone ID for local branch operations", example = "UTC")
    String timeZone,

    @Schema(description = "Active operation status", example = "true")
    boolean isActive
) {
    public static DealershipDto fromEntity(Dealership dealership) {
        return new DealershipDto(
            dealership.getId(),
            dealership.getCode(),
            dealership.getName(),
            dealership.getAddress(),
            dealership.getCity(),
            dealership.getOperatingOpenTime(),
            dealership.getOperatingCloseTime(),
            dealership.getTimeZone(),
            dealership.isActive()
        );
    }
}
