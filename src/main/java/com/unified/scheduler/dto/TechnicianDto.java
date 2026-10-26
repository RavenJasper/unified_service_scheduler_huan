package com.unified.scheduler.dto;

import com.unified.scheduler.domain.Technician;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Technician profile and certified capabilities")
public record TechnicianDto(
    @Schema(description = "Technician ID", example = "1")
    Long id,

    @Schema(description = "Parent dealership ID", example = "1")
    Long dealershipId,

    @Schema(description = "Parent dealership name", example = "Central Metro Dealership")
    String dealershipName,

    @Schema(description = "Unique employee identifier", example = "TECH-001")
    String employeeCode,

    @Schema(description = "First name", example = "Alex")
    String firstName,

    @Schema(description = "Last name", example = "Nguyen")
    String lastName,

    @Schema(description = "Full display name", example = "Alex Nguyen")
    String fullName,

    @Schema(description = "Contact email", example = "alex.nguyen@centraldealership.com")
    String email,

    @Schema(description = "Active employment status", example = "true")
    boolean isActive,

    @Schema(description = "List of verified skill certifications")
    List<TechnicianSkillDto> skills
) {
    public static TechnicianDto fromEntity(Technician tech) {
        List<TechnicianSkillDto> skillDtos = (tech.getSkills() != null)
            ? tech.getSkills().stream().map(TechnicianSkillDto::fromEntity).toList()
            : List.of();

        return new TechnicianDto(
            tech.getId(),
            tech.getDealership() != null ? tech.getDealership().getId() : null,
            tech.getDealership() != null ? tech.getDealership().getName() : null,
            tech.getEmployeeCode(),
            tech.getFirstName(),
            tech.getLastName(),
            tech.getFullName(),
            tech.getEmail(),
            tech.isActive(),
            skillDtos
        );
    }
}
