package com.unified.scheduler.repository;

import com.unified.scheduler.domain.AppointmentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentAuditLogRepository extends JpaRepository<AppointmentAuditLog, UUID> {

    List<AppointmentAuditLog> findByAppointmentIdOrderByCreatedAtDesc(UUID appointmentId);
}
