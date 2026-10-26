package com.unified.scheduler.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "technician_skills", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tech_skills_tech_skill", columnNames = {"technician_id", "skill_code"})
})
public class TechnicianSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Column(name = "skill_code", nullable = false, length = 50)
    private String skillCode;

    @Column(name = "certified_date", nullable = false)
    private LocalDate certifiedDate = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public TechnicianSkill() {
    }

    public TechnicianSkill(Long id, Technician technician, String skillCode, LocalDate certifiedDate) {
        this.id = id;
        this.technician = technician;
        this.skillCode = skillCode;
        this.certifiedDate = certifiedDate != null ? certifiedDate : LocalDate.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.certifiedDate == null) {
            this.certifiedDate = LocalDate.now();
        }
    }
}
