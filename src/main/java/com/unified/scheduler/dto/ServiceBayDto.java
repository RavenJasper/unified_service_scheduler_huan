package com.unified.scheduler.dto;

import com.unified.scheduler.domain.ServiceBay;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Physical service bay in a dealership workshop")
public record ServiceBayDto(
    @Schema(description = "Unique ID of the service bay", example = "1")
    Long id,

    @Schema(description = "ID of the parent dealership", example = "1")
    Long dealershipId,

    @Schema(description = "Name of the parent dealership", example = "Central Metro Dealership")
    String dealershipName,

    @Schema(description = "Bay number within the dealership", example = "1")
    int bayNumber,

    @Schema(description = "Human-readable bay name / station designation", example = "Bay 1 - Express Lift")
    String name,

    @Schema(description = "Operational status of the service bay", example = "true")
    boolean isOperational
) {
    public static ServiceBayDto fromEntity(ServiceBay bay) {
        return new ServiceBayDto(
            bay.getId(),
            bay.getDealership() != null ? bay.getDealership().getId() : null,
            bay.getDealership() != null ? bay.getDealership().getName() : null,
            bay.getBayNumber(),
            bay.getName(),
            bay.isOperational()
        );
    }
}
