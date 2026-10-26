package com.unified.scheduler.service;

import com.unified.scheduler.domain.*;
import com.unified.scheduler.dto.*;
import com.unified.scheduler.exception.InvalidBookingException;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.exception.ResourceUnavailableException;
import com.unified.scheduler.repository.*;
import com.unified.scheduler.repository.specification.AppointmentSpecifications;
import com.unified.scheduler.util.SlotCalculator;
import com.unified.scheduler.util.SpecificationUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SchedulingServiceImpl implements SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingServiceImpl.class);
    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);
    private static final long MAX_SEARCH_DAYS = 31;

    private final DealershipRepository dealershipRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceBayRepository serviceBayRepository;
    private final TechnicianRepository technicianRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentAuditLogRepository auditLogRepository;
    private final CustomerRepository customerRepository;
    private final SlotCalculator slotCalculator;

    private final Counter bookedCounter;
    private final Counter conflictCounter;
    private final Timer bookingTimer;

    public SchedulingServiceImpl(DealershipRepository dealershipRepository,
                                 ServiceTypeRepository serviceTypeRepository,
                                 ServiceBayRepository serviceBayRepository,
                                 TechnicianRepository technicianRepository,
                                 AppointmentRepository appointmentRepository,
                                 AppointmentAuditLogRepository auditLogRepository,
                                 CustomerRepository customerRepository,
                                 SlotCalculator slotCalculator,
                                 MeterRegistry meterRegistry) {
        this.dealershipRepository = dealershipRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.serviceBayRepository = serviceBayRepository;
        this.technicianRepository = technicianRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditLogRepository = auditLogRepository;
        this.customerRepository = customerRepository;
        this.slotCalculator = slotCalculator;

        this.bookedCounter = Counter.builder("appointments.booked.total")
            .description("Total successfully booked appointments")
            .tag("status", "success")
            .register(meterRegistry);

        this.conflictCounter = Counter.builder("appointments.rejected.conflict")
            .description("Total appointment requests rejected due to slot conflicts")
            .tag("reason", "slot_unavailable")
            .register(meterRegistry);

        this.bookingTimer = Timer.builder("scheduling.booking.duration")
            .description("Latency distribution for appointment booking critical path")
            .register(meterRegistry);
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityScheduleResponseDto getAvailableSlots(Long dealershipId, Long serviceTypeId, LocalDate startDate, LocalDate endDate, LocalTime desiredTime) {
        DateRange dateRange = resolveDateRange(startDate, endDate);
        log.info("Calculating available slots for dealership={}, serviceType={}, start={}, end={}, desiredTime={}",
            dealershipId, serviceTypeId, dateRange.start(), dateRange.end(), desiredTime);

        Dealership dealership = getActiveDealership(dealershipId);
        ServiceType serviceType = getActiveServiceType(serviceTypeId);

        var candidateBays = serviceBayRepository.findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(dealershipId);
        var candidateTechs = technicianRepository.findActiveTechniciansWithSkill(dealershipId, serviceType.getRequiredSkillCode());

        Map<LocalDate, List<AvailableSlotDto>> slotsByDate = calculateAvailableSlotsByDate(
            dealership, serviceType, dateRange, candidateBays, candidateTechs, desiredTime);

        List<DailyAvailabilityDto> days = buildDailyAvailabilities(dateRange, slotsByDate);
        int totalAvailableSlots = days.stream().mapToInt(DailyAvailabilityDto::availableSlotCount).sum();

        return new AvailabilityScheduleResponseDto(
            dealership.getId(),
            dealership.getName(),
            dealership.getTimeZone(),
            serviceType.getId(),
            serviceType.getName(),
            serviceType.getStandardDurationMinutes(),
            dateRange.start(),
            dateRange.end(),
            totalAvailableSlots,
            days
        );
    }

    @Override
    @Transactional
    public AppointmentResponseDto bookAppointment(BookingRequestDto request) {
        return bookingTimer.record(() -> {
            log.info("Processing booking request: customer={}, vin={}, dealership={}, serviceType={}, start={}",
                request.customerId(), request.vehicleVin(), request.dealershipId(), request.serviceTypeId(), request.requestedStartTime());

            Dealership dealership = getActiveDealership(request.dealershipId());
            ServiceType serviceType = getActiveServiceType(request.serviceTypeId());
            Customer customer = getActiveCustomer(request.customerId());

            OffsetDateTime startTime = request.requestedStartTime();
            int durationMinutes = serviceType.getStandardDurationMinutes();
            OffsetDateTime endTime = startTime.plusMinutes(durationMinutes);

            validateTimeWindow(dealership, startTime, endTime);
            validateCustomerAndVehicleAvailability(customer, request.vehicleVin(), startTime, endTime, null);

            AllocatedResources allocated = allocateResources(dealership, serviceType, startTime, endTime, null, request.technicianId());
            Appointment saved = createAndPersistAppointment(request, customer, dealership, serviceType, allocated, startTime, endTime, durationMinutes);

            log.info("Appointment booked successfully: ref={}, id={}, bay={}, tech={}",
                saved.getReferenceNumber(), saved.getId(), allocated.bay().getName(), allocated.technician().getFullName());

            return AppointmentResponseDto.fromEntity(saved);
        });
    }

    @Override
    @Transactional
    public AppointmentResponseDto cancelAppointment(UUID appointmentId, CancellationRequestDto request) {
        log.info("Processing cancellation request for appointmentId={}", appointmentId);
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        if (!appointment.getStatus().canBeCancelled()) {
            throw new InvalidBookingException("Cannot cancel appointment with status " + appointment.getStatus() +
                ". Only CONFIRMED appointments can be cancelled.");
        }

        String previousStatus = appointment.getStatus().name();
        appointment.cancel(request.reason());
        Appointment updated = appointmentRepository.save(appointment);

        recordAuditLog(updated.getId(), "CANCELLED", previousStatus, AppointmentStatus.CANCELLED.name(), request.reason());
        log.info("Appointment {} successfully cancelled. Resources released.", appointmentId);

        return AppointmentResponseDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public AppointmentResponseDto rescheduleAppointment(UUID appointmentId, RescheduleRequestDto request) {
        log.info("Processing reschedule request for appointmentId={}, newStart={}", appointmentId, request.newStartTime());
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        if (!appointment.getStatus().canBeRescheduled()) {
            throw new InvalidBookingException("Cannot reschedule appointment with status " + appointment.getStatus() +
                ". Only CONFIRMED appointments can be rescheduled.");
        }

        OffsetDateTime newStartTime = request.newStartTime();
        int durationMinutes = appointment.getDurationMinutes();
        OffsetDateTime newEndTime = newStartTime.plusMinutes(durationMinutes);

        validateTimeWindow(appointment.getDealership(), newStartTime, newEndTime);
        validateCustomerAndVehicleAvailability(appointment.getCustomer(), appointment.getVehicleVin(), newStartTime, newEndTime, appointment.getId());

        AllocatedResources allocated = allocateResources(
            appointment.getDealership(), appointment.getServiceType(), newStartTime, newEndTime, appointment.getId(), request.technicianId()
        );

        appointment.reschedule(allocated.bay(), allocated.technician(), newStartTime, newEndTime);
        Appointment saved = appointmentRepository.save(appointment);

        String reason = (request.reason() != null && !request.reason().isBlank()) ? request.reason() : "Rescheduled via API";
        recordAuditLog(saved.getId(), "RESCHEDULED", AppointmentStatus.CONFIRMED.name(), AppointmentStatus.CONFIRMED.name(), reason);
        log.info("Appointment {} successfully rescheduled to [{} - {}]", appointmentId, newStartTime, newEndTime);

        return AppointmentResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public AppointmentResponseDto reassignTechnician(UUID appointmentId, ReassignTechnicianRequestDto request) {
        log.info("Processing technician reassignment for appointmentId={}, targetTechId={}", appointmentId, request.technicianId());
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        if (!appointment.getStatus().isActive()) {
            throw new InvalidBookingException("Cannot reassign technician for appointment with status " + appointment.getStatus() +
                ". Only active appointments can be reassigned.");
        }

        Technician targetTech = validateAndLockTargetTechnician(appointment, request.technicianId());
        Technician previousTech = appointment.getTechnician();
        appointment.reassignTechnician(targetTech);
        Appointment saved = appointmentRepository.save(appointment);

        String reason = (request.reason() != null && !request.reason().isBlank()) 
            ? request.reason() 
            : "Reassigned from " + previousTech.getEmployeeCode() + " to " + targetTech.getEmployeeCode();
        recordAuditLog(saved.getId(), "REASSIGNED", saved.getStatus().name(), saved.getStatus().name(), reason);
        log.info("Appointment {} successfully reassigned from technician {} to technician {}",
            appointmentId, previousTech.getEmployeeCode(), targetTech.getEmployeeCode());

        return AppointmentResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDto getAppointmentById(UUID appointmentId) {
        return AppointmentResponseDto.fromEntity(findAppointmentOrThrow(appointmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDto getAppointmentByReference(String referenceNumber) {
        return appointmentRepository.findByReferenceNumber(referenceNumber)
            .map(AppointmentResponseDto::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with reference: " + referenceNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> searchAppointments(Long customerId, String vehicleVin, Long dealershipId, AppointmentStatus status) {
        if (customerId != null) {
            return appointmentRepository.findByCustomerIdOrderByStartTimeDesc(customerId)
                .stream().map(AppointmentResponseDto::fromEntity).toList();
        }
        if (vehicleVin != null && !vehicleVin.isBlank()) {
            return appointmentRepository.findByVehicleVinOrderByStartTimeDesc(vehicleVin)
                .stream().map(AppointmentResponseDto::fromEntity).toList();
        }
        if (dealershipId != null && status != null) {
            return appointmentRepository.findByDealershipIdAndStatusOrderByStartTimeDesc(dealershipId, status)
                .stream().map(AppointmentResponseDto::fromEntity).toList();
        }
        return appointmentRepository.findAll().stream().map(AppointmentResponseDto::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<AppointmentResponseDto> searchAppointmentsPaged(String query, Long dealershipId, Long customerId, Long technicianId, AppointmentStatus status, Pageable pageable) {
        log.info("Searching appointments with pagination: query={}, dealershipId={}, customerId={}, technicianId={}, status={}, pageable={}",
            query, dealershipId, customerId, technicianId, status, pageable);

        if (technicianId != null) {
            validateTechnicianBelongsToDealership(technicianId, dealershipId);
        }

        var spec = AppointmentSpecifications.filter(query, dealershipId, customerId, technicianId, status);
        var page = appointmentRepository.findAll(spec, pageable).map(AppointmentResponseDto::fromEntity);
        return PageResponseDto.fromPage(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentAuditLogDto> getAppointmentAuditLogs(UUID appointmentId) {
        log.info("Fetching audit logs for appointmentId={}", appointmentId);
        findAppointmentOrThrow(appointmentId);
        return auditLogRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId)
            .stream()
            .map(AppointmentAuditLogDto::fromEntity)
            .toList();
    }

    private record DateRange(LocalDate start, LocalDate end) {}
    private record AllocatedResources(ServiceBay bay, Technician technician) {}

    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.now();
        LocalDate end = (endDate != null) ? endDate : start.plusDays(6);

        if (end.isBefore(start)) {
            throw new InvalidBookingException("endDate cannot be before startDate");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_SEARCH_DAYS) {
            throw new InvalidBookingException("Availability search window cannot exceed " + MAX_SEARCH_DAYS + " days (1 month)");
        }
        return new DateRange(start, end);
    }

    private Dealership getActiveDealership(Long dealershipId) {
        var dealership = dealershipRepository.findById(dealershipId)
            .orElseThrow(() -> new ResourceNotFoundException("Dealership not found with ID: " + dealershipId + ". Please query GET /api/v1/dealerships to view valid dealerships."));
        if (!dealership.isActive()) {
            throw new InvalidBookingException("Dealership is inactive with ID: " + dealershipId);
        }
        return dealership;
    }

    private ServiceType getActiveServiceType(Long serviceTypeId) {
        var serviceType = serviceTypeRepository.findById(serviceTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Service type not found with ID: " + serviceTypeId + ". Please query GET /api/v1/service-types to view valid service types."));
        if (!serviceType.isActive()) {
            throw new InvalidBookingException("Service type is inactive with ID: " + serviceTypeId);
        }
        return serviceType;
    }

    private Customer getActiveCustomer(Long customerId) {
        var customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId + ". Please query GET /api/v1/customers to view active customer profiles (e.g. ID 1 for CUST-001 John Doe)."));
        if (!customer.isActive()) {
            throw new InvalidBookingException("Customer account is inactive with ID: " + customerId + ". Inactive customers cannot book service appointments.");
        }
        return customer;
    }

    private Appointment findAppointmentOrThrow(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));
    }

    private void validateTimeWindow(Dealership dealership, OffsetDateTime startTime, OffsetDateTime endTime) {
        if (!slotCalculator.isOnGrid(startTime)) {
            throw new InvalidBookingException("Requested start time must align with the 30-minute schedule grid (XX:00 or XX:30)");
        }
        if (!slotCalculator.isWithinOperatingHours(dealership, startTime, endTime)) {
            throw new InvalidBookingException("Requested appointment time [" + startTime + " - " + endTime +
                "] is outside dealership operating hours (" + dealership.getOperatingOpenTime() + " - " + dealership.getOperatingCloseTime() + ")");
        }
    }

    private void validateCustomerAndVehicleAvailability(Customer customer,
                                                        String vehicleVin,
                                                        OffsetDateTime startTime,
                                                        OffsetDateTime endTime,
                                                        UUID excludeAppointmentId) {
        validateCustomerAvailability(customer, startTime, endTime, excludeAppointmentId);
        validateVehicleAvailability(vehicleVin, startTime, endTime, excludeAppointmentId);
    }

    private void validateCustomerAvailability(Customer customer, OffsetDateTime startTime, OffsetDateTime endTime, UUID excludeAppointmentId) {
        long customerConflicts = (excludeAppointmentId != null)
            ? appointmentRepository.countConflictingCustomerAppointmentsExcluding(customer.getId(), excludeAppointmentId, startTime, endTime, ACTIVE_STATUSES)
            : appointmentRepository.countConflictingCustomerAppointments(customer.getId(), startTime, endTime, ACTIVE_STATUSES);

        if (customerConflicts > 0) {
            log.warn("Customer conflict detected for customer={} in window [{} - {}]", customer.getId(), startTime, endTime);
            conflictCounter.increment();
            throw new ResourceUnavailableException(String.format(
                "Customer %s (ID: %d) already has an active appointment scheduled during slot [%s - %s]. Simultaneous bookings for the same customer are not permitted.",
                customer.getFullName(), customer.getId(), startTime, endTime
            ));
        }
    }

    private void validateVehicleAvailability(String vehicleVin, OffsetDateTime startTime, OffsetDateTime endTime, UUID excludeAppointmentId) {
        long vehicleConflicts = (excludeAppointmentId != null)
            ? appointmentRepository.countConflictingVehicleAppointmentsExcluding(vehicleVin, excludeAppointmentId, startTime, endTime, ACTIVE_STATUSES)
            : appointmentRepository.countConflictingVehicleAppointments(vehicleVin, startTime, endTime, ACTIVE_STATUSES);

        if (vehicleConflicts > 0) {
            log.warn("Vehicle VIN conflict detected for vin={} in window [{} - {}]", vehicleVin, startTime, endTime);
            conflictCounter.increment();
            throw new ResourceUnavailableException(String.format(
                "Vehicle with VIN %s already has an active appointment scheduled during slot [%s - %s]. Simultaneous bookings for the same vehicle are not permitted.",
                vehicleVin, startTime, endTime
            ));
        }
    }

    private AllocatedResources allocateResources(Dealership dealership,
                                                  ServiceType serviceType,
                                                  OffsetDateTime startTime,
                                                  OffsetDateTime endTime,
                                                  UUID excludeAppointmentId,
                                                  Long preferredTechnicianId) {
        var candidateBays = serviceBayRepository.findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(dealership.getId());
        var candidateTechs = technicianRepository.findActiveTechniciansWithSkill(dealership.getId(), serviceType.getRequiredSkillCode());

        if (candidateBays.isEmpty() || candidateTechs.isEmpty()) {
            conflictCounter.increment();
            throw new ResourceUnavailableException("No operational bays or qualified technicians available at this dealership for the requested service");
        }

        validatePreferredTechnician(candidateTechs, preferredTechnicianId, dealership, serviceType);

        List<Long> sortedBayIds = candidateBays.stream().map(ServiceBay::getId).sorted().toList();
        List<Long> sortedTechIds = candidateTechs.stream().map(Technician::getId).sorted().toList();

        var lockedBays = serviceBayRepository.findAllByIdWithLock(sortedBayIds);
        var lockedTechs = technicianRepository.findAllByIdWithLock(sortedTechIds);

        ServiceBay selectedBay = selectFreeBay(lockedBays, startTime, endTime, excludeAppointmentId);
        Technician selectedTech = selectFreeTechnician(lockedTechs, startTime, endTime, excludeAppointmentId, preferredTechnicianId);

        if (selectedBay == null || selectedTech == null) {
            log.warn("Slot conflict detected for requested window [{} - {}]: bayFound={}, techFound={}",
                startTime, endTime, selectedBay != null, selectedTech != null);
            conflictCounter.increment();
            throw new ResourceUnavailableException("No service bay or qualified technician is available for slot [" +
                startTime + " - " + endTime + "]");
        }

        return new AllocatedResources(selectedBay, selectedTech);
    }

    private void validatePreferredTechnician(List<Technician> candidateTechs, Long preferredTechnicianId, Dealership dealership, ServiceType serviceType) {
        if (preferredTechnicianId != null) {
            boolean isQualified = candidateTechs.stream().anyMatch(t -> t.getId().equals(preferredTechnicianId));
            if (!isQualified) {
                throw new InvalidBookingException("Requested technician with ID " + preferredTechnicianId +
                    " is not active, does not possess required skill " + serviceType.getRequiredSkillCode() +
                    ", or does not belong to dealership " + dealership.getName());
            }
        }
    }

    private ServiceBay selectFreeBay(List<ServiceBay> lockedBays, OffsetDateTime startTime, OffsetDateTime endTime, UUID excludeAppointmentId) {
        return lockedBays.stream()
            .filter(bay -> isBayFree(bay.getId(), startTime, endTime, excludeAppointmentId))
            .findFirst()
            .orElse(null);
    }

    private Technician selectFreeTechnician(List<Technician> lockedTechs, OffsetDateTime startTime, OffsetDateTime endTime, UUID excludeAppointmentId, Long preferredTechnicianId) {
        if (preferredTechnicianId != null) {
            Technician preferredTech = lockedTechs.stream()
                .filter(tech -> tech.getId().equals(preferredTechnicianId))
                .filter(tech -> isTechFree(tech.getId(), startTime, endTime, excludeAppointmentId))
                .findFirst()
                .orElse(null);
            if (preferredTech == null) {
                log.warn("Preferred technician {} is busy during requested window [{} - {}]", preferredTechnicianId, startTime, endTime);
                conflictCounter.increment();
                throw new ResourceUnavailableException("Requested technician " + preferredTechnicianId +
                    " is unavailable during slot [" + startTime + " - " + endTime + "]");
            }
            return preferredTech;
        }

        return lockedTechs.stream()
            .filter(tech -> isTechFree(tech.getId(), startTime, endTime, excludeAppointmentId))
            .findFirst()
            .orElse(null);
    }

    private Appointment createAndPersistAppointment(BookingRequestDto request,
                                                    Customer customer,
                                                    Dealership dealership,
                                                    ServiceType serviceType,
                                                    AllocatedResources allocated,
                                                    OffsetDateTime startTime,
                                                    OffsetDateTime endTime,
                                                    int durationMinutes) {
        String referenceNumber = generateReferenceNumber(startTime);
        Appointment appointment = new Appointment(
            null,
            referenceNumber,
            customer,
            request.vehicleVin(),
            dealership,
            serviceType,
            allocated.bay(),
            allocated.technician(),
            startTime,
            endTime,
            durationMinutes
        );

        Appointment saved = appointmentRepository.save(appointment);
        recordAuditLog(saved.getId(), "BOOKED", null, AppointmentStatus.CONFIRMED.name(), "Initial booking created via REST API");
        bookedCounter.increment();
        return saved;
    }

    private Technician validateAndLockTargetTechnician(Appointment appointment, Long targetTechId) {
        Technician targetTech = technicianRepository.findByIdWithSkills(targetTechId)
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + targetTechId));

        if (!targetTech.isActive()) {
            throw new InvalidBookingException("Technician " + targetTech.getEmployeeCode() + " is currently inactive.");
        }

        if (!targetTech.getDealership().getId().equals(appointment.getDealership().getId())) {
            throw new InvalidBookingException("Technician " + targetTech.getEmployeeCode() + 
                " does not belong to dealership " + appointment.getDealership().getName());
        }

        String requiredSkill = appointment.getServiceType().getRequiredSkillCode();
        if (!targetTech.hasSkill(requiredSkill)) {
            throw new InvalidBookingException("Technician " + targetTech.getEmployeeCode() + 
                " does not possess the required skill " + requiredSkill + " for service " + appointment.getServiceType().getName());
        }

        technicianRepository.findByIdWithLock(targetTechId);

        if (!isTechFree(targetTechId, appointment.getStartTime(), appointment.getEndTime(), appointment.getId())) {
            conflictCounter.increment();
            throw new ResourceUnavailableException("Technician " + targetTech.getFullName() + 
                " is unavailable during slot [" + appointment.getStartTime() + " - " + appointment.getEndTime() + "]");
        }

        return targetTech;
    }

    private void validateTechnicianBelongsToDealership(Long technicianId, Long dealershipId) {
        Technician technician = technicianRepository.findById(technicianId)
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + technicianId));

        if (dealershipId != null && !technician.getDealership().getId().equals(dealershipId)) {
            throw new InvalidBookingException("Technician " + technician.getEmployeeCode() +
                " does not belong to dealership with ID " + dealershipId);
        }
    }

    private boolean isBayFree(Long bayId, OffsetDateTime start, OffsetDateTime end, UUID excludeId) {
        long conflicts = (excludeId != null)
            ? appointmentRepository.countConflictingBayAppointmentsExcluding(bayId, excludeId, start, end, ACTIVE_STATUSES)
            : appointmentRepository.countConflictingBayAppointments(bayId, start, end, ACTIVE_STATUSES);
        return conflicts == 0;
    }

    private boolean isTechFree(Long techId, OffsetDateTime start, OffsetDateTime end, UUID excludeId) {
        long conflicts = (excludeId != null)
            ? appointmentRepository.countConflictingTechnicianAppointmentsExcluding(techId, excludeId, start, end, ACTIVE_STATUSES)
            : appointmentRepository.countConflictingTechnicianAppointments(techId, start, end, ACTIVE_STATUSES);
        return conflicts == 0;
    }

    private Map<LocalDate, List<AvailableSlotDto>> calculateAvailableSlotsByDate(Dealership dealership,
                                                                                 ServiceType serviceType,
                                                                                 DateRange dateRange,
                                                                                 List<ServiceBay> candidateBays,
                                                                                 List<Technician> candidateTechs,
                                                                                 LocalTime desiredTime) {
        if (candidateBays.isEmpty() || candidateTechs.isEmpty()) {
            log.warn("Dealership {} has 0 bays ({}) or 0 qualified techs ({}) for skill {}",
                dealership.getId(), candidateBays.size(), candidateTechs.size(), serviceType.getRequiredSkillCode());
            return Collections.emptyMap();
        }

        int duration = serviceType.getStandardDurationMinutes();
        List<OffsetDateTime> slots = generateCandidateSlots(dealership, dateRange, duration, desiredTime);
        if (slots.isEmpty()) {
            return Collections.emptyMap();
        }

        OffsetDateTime windowStart = slots.getFirst();
        OffsetDateTime windowEnd = slots.getLast().plusMinutes(duration);
        var activeAppointments = appointmentRepository.findActiveAppointmentsInWindow(
            dealership.getId(), windowStart, windowEnd, ACTIVE_STATUSES);

        List<AvailableSlotDto> availableSlots = evaluateSlotAvailability(
            slots, duration, candidateBays, candidateTechs, activeAppointments);

        return groupSlotsByLocalDate(availableSlots, ZoneId.of(dealership.getTimeZone()));
    }

    private Map<LocalDate, List<AvailableSlotDto>> groupSlotsByLocalDate(List<AvailableSlotDto> availableSlots, ZoneId zoneId) {
        Map<LocalDate, List<AvailableSlotDto>> slotsByDate = new LinkedHashMap<>();
        for (AvailableSlotDto slot : availableSlots) {
            LocalDate slotDate = slot.startTime().atZoneSameInstant(zoneId).toLocalDate();
            slotsByDate.computeIfAbsent(slotDate, k -> new ArrayList<>()).add(slot);
        }
        return slotsByDate;
    }

    private List<DailyAvailabilityDto> buildDailyAvailabilities(DateRange dateRange, Map<LocalDate, List<AvailableSlotDto>> slotsByDate) {
        List<DailyAvailabilityDto> days = new ArrayList<>();
        LocalDate curr = dateRange.start();
        while (!curr.isAfter(dateRange.end())) {
            List<AvailableSlotDto> daySlots = slotsByDate.getOrDefault(curr, Collections.emptyList());
            days.add(new DailyAvailabilityDto(curr, curr.getDayOfWeek(), daySlots.size(), daySlots));
            curr = curr.plusDays(1);
        }
        return days;
    }

    private List<OffsetDateTime> generateCandidateSlots(Dealership dealership, DateRange range, int duration, LocalTime desiredTime) {
        ZoneId zone = ZoneId.of(dealership.getTimeZone() != null ? dealership.getTimeZone() : "UTC");
        OffsetDateTime now = OffsetDateTime.now(zone);

        List<OffsetDateTime> candidateStarts = new ArrayList<>();
        LocalDate curDate = range.start();
        while (!curDate.isAfter(range.end())) {
            var dayStarts = slotCalculator.generateCandidateStartTimes(dealership, curDate, duration);
            for (OffsetDateTime start : dayStarts) {
                if (start.isBefore(now)) {
                    continue;
                }
                if (desiredTime != null && !start.atZoneSameInstant(zone).toLocalTime().equals(desiredTime)) {
                    continue;
                }
                candidateStarts.add(start);
            }
            curDate = curDate.plusDays(1);
        }
        return candidateStarts;
    }

    private List<AvailableSlotDto> evaluateSlotAvailability(List<OffsetDateTime> candidateStarts,
                                                            int duration,
                                                            List<ServiceBay> candidateBays,
                                                            List<Technician> candidateTechs,
                                                            List<Appointment> activeAppointments) {
        List<AvailableSlotDto> results = new ArrayList<>();
        for (OffsetDateTime start : candidateStarts) {
            evaluateSingleSlot(start, duration, candidateBays, candidateTechs, activeAppointments)
                .ifPresent(results::add);
        }
        return results;
    }

    private Optional<AvailableSlotDto> evaluateSingleSlot(OffsetDateTime start,
                                                          int duration,
                                                          List<ServiceBay> candidateBays,
                                                          List<Technician> candidateTechs,
                                                          List<Appointment> activeAppointments) {
        OffsetDateTime end = start.plusMinutes(duration);

        Set<Long> busyBayIds = new HashSet<>();
        Set<Long> busyTechIds = new HashSet<>();

        for (Appointment app : activeAppointments) {
            if (slotCalculator.overlaps(app.getStartTime(), app.getEndTime(), start, end)) {
                busyBayIds.add(app.getServiceBay().getId());
                busyTechIds.add(app.getTechnician().getId());
            }
        }

        List<AvailableBayDto> freeBays = candidateBays.stream()
            .filter(b -> !busyBayIds.contains(b.getId()))
            .map(b -> new AvailableBayDto(b.getId(), b.getBayNumber(), b.getName()))
            .toList();

        List<AvailableTechnicianDto> freeTechs = candidateTechs.stream()
            .filter(t -> !busyTechIds.contains(t.getId()))
            .map(t -> new AvailableTechnicianDto(t.getId(), t.getEmployeeCode(), t.getFullName()))
            .toList();

        int availableCapacity = Math.min(freeBays.size(), freeTechs.size());
        if (availableCapacity <= 0) {
            return Optional.empty();
        }

        String formattedTime = String.format("%02d:%02d - %02d:%02d",
            start.getHour(), start.getMinute(), end.getHour(), end.getMinute());
        return Optional.of(new AvailableSlotDto(start, end, formattedTime, availableCapacity, freeTechs, freeBays));
    }

    private void recordAuditLog(UUID appointmentId, String action, String fromStatus, String toStatus, String notes) {
        AppointmentAuditLog auditLog = new AppointmentAuditLog(
            appointmentId,
            action,
            fromStatus,
            toStatus,
            notes,
            "SYSTEM"
        );
        auditLogRepository.save(auditLog);
    }

    private String generateReferenceNumber(OffsetDateTime startTime) {
        String datePart = startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "APP-" + datePart + "-" + randomSuffix;
    }
}
