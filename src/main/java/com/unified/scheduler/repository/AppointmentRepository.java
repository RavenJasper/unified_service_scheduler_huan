package com.unified.scheduler.repository;

import com.unified.scheduler.domain.Appointment;
import com.unified.scheduler.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByReferenceNumber(String referenceNumber);

    @Query("SELECT a FROM Appointment a WHERE a.customer.id = :customerId ORDER BY a.startTime DESC")
    List<Appointment> findByCustomerIdOrderByStartTimeDesc(@Param("customerId") Long customerId);

    List<Appointment> findByVehicleVinOrderByStartTimeDesc(String vehicleVin);

    List<Appointment> findByDealershipIdAndStatusOrderByStartTimeDesc(Long dealershipId, AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.serviceBay.id = :bayId " +
           "AND a.status IN :activeStatuses " +
           "AND a.startTime < :endTime " +
           "AND a.endTime > :startTime")
    long countConflictingBayAppointments(@Param("bayId") Long bayId,
                                         @Param("startTime") OffsetDateTime startTime,
                                         @Param("endTime") OffsetDateTime endTime,
                                         @Param("activeStatuses") List<AppointmentStatus> activeStatuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.technician.id = :technicianId " +
           "AND a.status IN :activeStatuses " +
           "AND a.startTime < :endTime " +
           "AND a.endTime > :startTime")
    long countConflictingTechnicianAppointments(@Param("technicianId") Long technicianId,
                                                @Param("startTime") OffsetDateTime startTime,
                                                @Param("endTime") OffsetDateTime endTime,
                                                @Param("activeStatuses") List<AppointmentStatus> activeStatuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.serviceBay.id = :bayId " +
           "AND a.id <> :excludeAppointmentId " +
           "AND a.status IN :activeStatuses " +
           "AND a.startTime < :endTime " +
           "AND a.endTime > :startTime")
    long countConflictingBayAppointmentsExcluding(@Param("bayId") Long bayId,
                                                  @Param("excludeAppointmentId") UUID excludeAppointmentId,
                                                  @Param("startTime") OffsetDateTime startTime,
                                                  @Param("endTime") OffsetDateTime endTime,
                                                  @Param("activeStatuses") List<AppointmentStatus> activeStatuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.technician.id = :technicianId " +
           "AND a.id <> :excludeAppointmentId " +
           "AND a.status IN :activeStatuses " +
           "AND a.startTime < :endTime " +
           "AND a.endTime > :startTime")
    long countConflictingTechnicianAppointmentsExcluding(@Param("technicianId") Long technicianId,
                                                         @Param("excludeAppointmentId") UUID excludeAppointmentId,
                                                         @Param("startTime") OffsetDateTime startTime,
                                                         @Param("endTime") OffsetDateTime endTime,
                                                         @Param("activeStatuses") List<AppointmentStatus> activeStatuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.customer.id = :customerId " +
           "AND a.status IN :activeStatuses " +
           "AND a.startTime < :endTime " +
           "AND a.endTime > :startTime")
    long countConflictingCustomerAppointments(@Param("customerId") Long customerId,
                                              @Param("startTime") OffsetDateTime startTime,
                                              @Param("endTime") OffsetDateTime endTime,
                                              @Param("activeStatuses") List<AppointmentStatus> activeStatuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.customer.id = :customerId " +
           "AND a.id <> :excludeAppointmentId " +
           "AND a.status IN :activeStatuses " +
           "AND a.startTime < :endTime " +
           "AND a.endTime > :startTime")
    long countConflictingCustomerAppointmentsExcluding(@Param("customerId") Long customerId,
                                                       @Param("excludeAppointmentId") UUID excludeAppointmentId,
                                                       @Param("startTime") OffsetDateTime startTime,
                                                       @Param("endTime") OffsetDateTime endTime,
                                                       @Param("activeStatuses") List<AppointmentStatus> activeStatuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.vehicleVin = :vehicleVin " +
           "AND a.status IN :activeStatuses " +
           "AND a.startTime < :endTime " +
           "AND a.endTime > :startTime")
    long countConflictingVehicleAppointments(@Param("vehicleVin") String vehicleVin,
                                             @Param("startTime") OffsetDateTime startTime,
                                             @Param("endTime") OffsetDateTime endTime,
                                             @Param("activeStatuses") List<AppointmentStatus> activeStatuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.vehicleVin = :vehicleVin " +
           "AND a.id <> :excludeAppointmentId " +
           "AND a.status IN :activeStatuses " +
           "AND a.startTime < :endTime " +
           "AND a.endTime > :startTime")
    long countConflictingVehicleAppointmentsExcluding(@Param("vehicleVin") String vehicleVin,
                                                      @Param("excludeAppointmentId") UUID excludeAppointmentId,
                                                      @Param("startTime") OffsetDateTime startTime,
                                                      @Param("endTime") OffsetDateTime endTime,
                                                      @Param("activeStatuses") List<AppointmentStatus> activeStatuses);

    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.serviceBay " +
           "JOIN FETCH a.technician " +
           "WHERE a.dealership.id = :dealershipId " +
           "AND a.status IN :activeStatuses " +
           "AND a.startTime < :windowEnd " +
           "AND a.endTime > :windowStart")
    List<Appointment> findActiveAppointmentsInWindow(@Param("dealershipId") Long dealershipId,
                                                     @Param("windowStart") OffsetDateTime windowStart,
                                                     @Param("windowEnd") OffsetDateTime windowEnd,
                                                     @Param("activeStatuses") List<AppointmentStatus> activeStatuses);
}
