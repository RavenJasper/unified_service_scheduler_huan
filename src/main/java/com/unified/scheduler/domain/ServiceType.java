package com.unified.scheduler.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "service_types")
public class ServiceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "standard_duration_minutes", nullable = false)
    private int standardDurationMinutes;

    @Column(name = "required_skill_code", nullable = false, length = 50)
    private String requiredSkillCode;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public ServiceType() {
    }

    public ServiceType(Long id, String code, String name, String description,
                       int standardDurationMinutes, String requiredSkillCode) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.standardDurationMinutes = standardDurationMinutes;
        this.requiredSkillCode = requiredSkillCode;
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
