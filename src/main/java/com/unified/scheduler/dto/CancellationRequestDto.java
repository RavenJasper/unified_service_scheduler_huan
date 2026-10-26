package com.unified.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload to cancel an active appointment")
public record CancellationRequestDto(
    @NotBlank(message = "Cancellation reason must not be blank")
    @JsonAlias({"cancellationReason", "reason"})
    @Schema(description = "Explanation for cancelling the appointment", example = "Customer sold the vehicle")
    String reason
) {}
