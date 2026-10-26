package com.unified.scheduler.dto;

import com.unified.scheduler.domain.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Service catalogue item")
public record ServiceTypeDto(
    @Schema(description = "Numeric identifier of service type", example = "2")
    Long id,

    @Schema(description = "Business code", example = "BRAKE_SERVICE")
    String code,

    @Schema(description = "Service name", example = "Comprehensive Brake System Service")
    String name,

    @Schema(description = "Detailed service description", example = "Full pad and rotor inspection and replacement")
    String description,

    @Schema(description = "Standard service duration in minutes", example = "60")
    int standardDurationMinutes,

    @Schema(description = "Skill qualification required to perform this service", example = "BRAKE_SPECIALIST")
    String requiredSkillCode,

    @Schema(description = "Active catalog status", example = "true")
    boolean isActive
) {
    public static ServiceTypeDto fromEntity(ServiceType serviceType) {
        return new ServiceTypeDto(
            serviceType.getId(),
            serviceType.getCode(),
            serviceType.getName(),
            serviceType.getDescription(),
            serviceType.getStandardDurationMinutes(),
            serviceType.getRequiredSkillCode(),
            serviceType.isActive()
        );
    }
}
