package com.unified.scheduler.dto;

import com.unified.scheduler.domain.Appointment;
import com.unified.scheduler.domain.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Representation of a scheduled service appointment")
public record AppointmentResponseDto(
    @Schema(description = "Internal unique UUID of the appointment")
    UUID id,

    @Schema(description = "Human-readable external reference code", example = "APP-20261101-A1B2C3")
    String referenceNumber,

    @Schema(description = "Customer ID", example = "1")
    Long customerId,

    @Schema(description = "Customer full name", example = "John Doe")
    String customerName,

    @Schema(description = "Customer email address", example = "john.doe@example.com")
    String customerEmail,

    @Schema(description = "Customer phone number", example = "+1-555-0101")
    String customerPhone,

    @Schema(description = "Vehicle VIN")
    String vehicleVin,

    @Schema(description = "Dealership ID", example = "1")
    Long dealershipId,

    @Schema(description = "Dealership name")
    String dealershipName,

    @Schema(description = "Service type ID", example = "2")
    Long serviceTypeId,

    @Schema(description = "Service type name")
    String serviceTypeName,

    @Schema(description = "Assigned service bay ID", example = "1")
    Long serviceBayId,

    @Schema(description = "Assigned service bay name", example = "Bay 1 - Express Lift")
    String serviceBayName,

    @Schema(description = "Assigned technician ID", example = "1")
    Long technicianId,

    @Schema(description = "Assigned technician full name", example = "Alex Nguyen")
    String technicianName,

    @Schema(description = "Scheduled start time with UTC offset")
    OffsetDateTime startTime,

    @Schema(description = "Scheduled end time with UTC offset")
    OffsetDateTime endTime,

    @Schema(description = "Standard duration in minutes", example = "60")
    int durationMinutes,

    @Schema(description = "Current lifecycle status", example = "CONFIRMED")
    AppointmentStatus status,

    @Schema(description = "Cancellation reason if cancelled")
    String cancellationReason,

    @Schema(description = "Creation timestamp")
    OffsetDateTime createdAt,

    @Schema(description = "Last update timestamp")
    OffsetDateTime updatedAt
) {
    public static AppointmentResponseDto fromEntity(Appointment appointment) {
        return new AppointmentResponseDto(
            appointment.getId(),
            appointment.getReferenceNumber(),
            appointment.getCustomerId(),
            appointment.getCustomer() != null ? appointment.getCustomer().getFullName() : null,
            appointment.getCustomer() != null ? appointment.getCustomer().getEmail() : null,
            appointment.getCustomer() != null ? appointment.getCustomer().getPhone() : null,
            appointment.getVehicleVin(),
            appointment.getDealership().getId(),
            appointment.getDealership().getName(),
            appointment.getServiceType().getId(),
            appointment.getServiceType().getName(),
            appointment.getServiceBay().getId(),
            appointment.getServiceBay().getName(),
            appointment.getTechnician().getId(),
            appointment.getTechnician().getFullName(),
            appointment.getStartTime(),
            appointment.getEndTime(),
            appointment.getDurationMinutes(),
            appointment.getStatus(),
            appointment.getCancellationReason(),
            appointment.getCreatedAt(),
            appointment.getUpdatedAt()
        );
    }
}
