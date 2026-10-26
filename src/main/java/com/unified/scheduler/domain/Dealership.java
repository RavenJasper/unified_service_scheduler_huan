package com.unified.scheduler.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "dealerships")
public class Dealership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "operating_open_time", nullable = false)
    private LocalTime operatingOpenTime;

    @Column(name = "operating_close_time", nullable = false)
    private LocalTime operatingCloseTime;

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Dealership() {
    }

    public Dealership(Long id, String code, String name, String address, String city,
                      LocalTime operatingOpenTime, LocalTime operatingCloseTime, String timeZone) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.address = address;
        this.city = city;
        this.operatingOpenTime = operatingOpenTime;
        this.operatingCloseTime = operatingCloseTime;
        this.timeZone = timeZone != null ? timeZone : "UTC";
        this.isActive = true;
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
