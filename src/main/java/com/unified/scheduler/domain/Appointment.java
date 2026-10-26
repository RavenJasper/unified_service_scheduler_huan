package com.unified.scheduler.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointments_bay_time", columnList = "service_bay_id, start_time, end_time"),
    @Index(name = "idx_appointments_tech_time", columnList = "technician_id, start_time, end_time"),
    @Index(name = "idx_appointments_dealer_time", columnList = "dealership_id, start_time, end_time"),
    @Index(name = "idx_appointments_customer", columnList = "customer_id"),
    @Index(name = "idx_appointments_vin", columnList = "vehicle_vin"),
    @Index(name = "idx_appointments_status", columnList = "status")
})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 32)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "vehicle_vin", nullable = false, length = 17)
    private String vehicleVin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealership_id", nullable = false)
    private Dealership dealership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_type_id", nullable = false)
    private ServiceType serviceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_bay_id", nullable = false)
    private ServiceBay serviceBay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Appointment() {
    }

    public Appointment(UUID id, String referenceNumber, Customer customer, String vehicleVin,
                       Dealership dealership, ServiceType serviceType, ServiceBay serviceBay,
                       Technician technician, OffsetDateTime startTime, OffsetDateTime endTime,
                       int durationMinutes) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.customer = customer;
        this.vehicleVin = vehicleVin;
        this.dealership = dealership;
        this.serviceType = serviceType;
        this.serviceBay = serviceBay;
        this.technician = technician;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.status = AppointmentStatus.CONFIRMED;
    }

    @PrePersist
    protected void onCreate() {
        var now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isActive() {
        return this.status.isActive();
    }

    public void cancel(String reason) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    public void reschedule(ServiceBay newBay, Technician newTechnician,
                           OffsetDateTime newStartTime, OffsetDateTime newEndTime) {
        this.serviceBay = newBay;
        this.technician = newTechnician;
        this.startTime = newStartTime;
        this.endTime = newEndTime;
        this.status = AppointmentStatus.CONFIRMED;
        this.cancellationReason = null;
    }

    public void reassignTechnician(Technician newTechnician) {
        this.technician = Objects.requireNonNull(newTechnician, "newTechnician must not be null");
    }

    public Long getCustomerId() {
        return customer != null ? customer.getId() : null;
    }
}
