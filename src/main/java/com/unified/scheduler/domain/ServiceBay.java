package com.unified.scheduler.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "service_bays", uniqueConstraints = {
    @UniqueConstraint(name = "uk_service_bays_dealer_number", columnNames = {"dealership_id", "bay_number"})
})
public class ServiceBay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealership_id", nullable = false)
    private Dealership dealership;

    @Column(name = "bay_number", nullable = false)
    private int bayNumber;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_operational", nullable = false)
    private boolean isOperational = true;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public ServiceBay() {
    }

    public ServiceBay(Long id, Dealership dealership, int bayNumber, String name) {
        this.id = id;
        this.dealership = dealership;
        this.bayNumber = bayNumber;
        this.name = name;
        this.isOperational = true;
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
}
