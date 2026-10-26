package com.unified.scheduler.service;

import com.unified.scheduler.domain.AppointmentStatus;
import com.unified.scheduler.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface SchedulingService {

    AvailabilityScheduleResponseDto getAvailableSlots(Long dealershipId, Long serviceTypeId, LocalDate startDate, LocalDate endDate, LocalTime desiredTime);

    AppointmentResponseDto bookAppointment(BookingRequestDto request);

    AppointmentResponseDto cancelAppointment(UUID appointmentId, CancellationRequestDto request);

    AppointmentResponseDto rescheduleAppointment(UUID appointmentId, RescheduleRequestDto request);

    AppointmentResponseDto reassignTechnician(UUID appointmentId, ReassignTechnicianRequestDto request);

    AppointmentResponseDto getAppointmentById(UUID appointmentId);

    AppointmentResponseDto getAppointmentByReference(String referenceNumber);

    List<AppointmentResponseDto> searchAppointments(Long customerId, String vehicleVin, Long dealershipId, AppointmentStatus status);

    PageResponseDto<AppointmentResponseDto> searchAppointmentsPaged(String query, Long dealershipId, Long customerId, Long technicianId, AppointmentStatus status, Pageable pageable);

    List<AppointmentAuditLogDto> getAppointmentAuditLogs(UUID appointmentId);
}
