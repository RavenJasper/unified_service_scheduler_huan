package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lightweight option item tailored for frontend dropdowns and autocomplete components")
public record LookupOptionDto(
    @Schema(description = "Primary key identifier (used as select value)", example = "1")
    Long id,

    @Schema(description = "Natural business code", example = "CUST-001")
    String code,

    @Schema(description = "Formatted display label rendered in UI dropdown", example = "John Doe (CUST-001)")
    String label
) {}
