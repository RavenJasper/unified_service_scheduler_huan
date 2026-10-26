package com.unified.scheduler.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "appointment_audit_logs", indexes = {
    @Index(name = "idx_audit_appointment_id", columnList = "appointment_id"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
public class AppointmentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "status_from", length = 30)
    private String statusFrom;

    @Column(name = "status_to", nullable = false, length = 30)
    private String statusTo;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy = "SYSTEM";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AppointmentAuditLog() {
    }

    public AppointmentAuditLog(UUID appointmentId, String action, String statusFrom,
                               String statusTo, String reason, String performedBy) {
        this.appointmentId = appointmentId;
        this.action = action;
        this.statusFrom = statusFrom;
        this.statusTo = statusTo;
        this.reason = reason;
        this.performedBy = performedBy != null ? performedBy : "SYSTEM";
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
