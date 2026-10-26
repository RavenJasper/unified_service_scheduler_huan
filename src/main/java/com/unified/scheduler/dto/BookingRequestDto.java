package com.unified.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

@Schema(description = "Request payload to schedule a new service appointment")
public record BookingRequestDto(
    @NotNull(message = "Customer ID is required")
    @Schema(description = "Unique ID of the customer", example = "1")
    Long customerId,

    @NotBlank(message = "Vehicle VIN is required")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "Vehicle VIN must be a valid 17-character alphanumeric string (excluding I, O, Q)")
    @Schema(description = "17-character ISO Vehicle Identification Number", example = "1HGCR2F83HA000001")
    String vehicleVin,

    @NotNull(message = "Dealership ID is required")
    @Schema(description = "ID of the dealership hosting the service", example = "1")
    Long dealershipId,

    @NotNull(message = "Service Type ID is required")
    @Schema(description = "ID of the requested service type", example = "2")
    Long serviceTypeId,

    @NotNull(message = "Requested start time is required")
    @Future(message = "Requested start time must be in the future")
    @Schema(description = "Desired appointment start time with UTC offset", example = "2026-11-01T09:00:00Z")
    OffsetDateTime requestedStartTime,

    @Schema(description = "Optional preferred technician ID for this appointment", example = "1")
    Long technicianId
) {
    public BookingRequestDto(Long customerId, String vehicleVin, Long dealershipId, Long serviceTypeId, OffsetDateTime requestedStartTime) {
        this(customerId, vehicleVin, dealershipId, serviceTypeId, requestedStartTime, null);
    }
}
