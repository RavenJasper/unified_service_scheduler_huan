package com.unified.scheduler.dto;

import com.unified.scheduler.domain.TechnicianSkill;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Technician certification skill credential")
public record TechnicianSkillDto(
    @Schema(description = "Skill entry ID", example = "1")
    Long id,

    @Schema(description = "Certified skill code", example = "BRAKE_SPECIALIST")
    String skillCode,

    @Schema(description = "Date when certification was awarded", example = "2024-01-15")
    LocalDate certifiedDate
) {
    public static TechnicianSkillDto fromEntity(TechnicianSkill skill) {
        return new TechnicianSkillDto(
            skill.getId(),
            skill.getSkillCode(),
            skill.getCertifiedDate()
        );
    }
}
