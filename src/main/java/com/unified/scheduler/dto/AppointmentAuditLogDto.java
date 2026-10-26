package com.unified.scheduler.dto;

import com.unified.scheduler.domain.AppointmentAuditLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Audit trail entry recording state transition or modification of an appointment")
public record AppointmentAuditLogDto(
    @Schema(description = "Unique UUID of the audit log entry")
    UUID id,

    @Schema(description = "Target appointment UUID")
    UUID appointmentId,

    @Schema(description = "Action performed (e.g. BOOKED, RESCHEDULED, CANCELLED, REASSIGNED)", example = "BOOKED")
    String action,

    @Schema(description = "Previous appointment status", example = "CONFIRMED")
    String statusFrom,

    @Schema(description = "New appointment status", example = "RESCHEDULED")
    String statusTo,

    @Schema(description = "Reason or operational notes explaining the action", example = "Customer requested earlier slot")
    String reason,

    @Schema(description = "Identity or system that performed the action", example = "SYSTEM")
    String performedBy,

    @Schema(description = "Timestamp when the audit log was recorded")
    OffsetDateTime createdAt
) {
    public static AppointmentAuditLogDto fromEntity(AppointmentAuditLog log) {
        return new AppointmentAuditLogDto(
            log.getId(),
            log.getAppointmentId(),
            log.getAction(),
            log.getStatusFrom(),
            log.getStatusTo(),
            log.getReason(),
            log.getPerformedBy(),
            log.getCreatedAt()
        );
    }
}
