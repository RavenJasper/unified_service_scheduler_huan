package com.unified.scheduler.service;

import com.unified.scheduler.domain.*;
import com.unified.scheduler.dto.BookingRequestDto;
import com.unified.scheduler.dto.CancellationRequestDto;
import com.unified.scheduler.dto.RescheduleRequestDto;
import com.unified.scheduler.exception.InvalidBookingException;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.exception.ResourceUnavailableException;
import com.unified.scheduler.repository.*;
import com.unified.scheduler.util.SlotCalculator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock
    private DealershipRepository dealershipRepository;
    @Mock
    private ServiceTypeRepository serviceTypeRepository;
    @Mock
    private ServiceBayRepository serviceBayRepository;
    @Mock
    private TechnicianRepository technicianRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentAuditLogRepository auditLogRepository;
    @Mock
    private CustomerRepository customerRepository;

    private SlotCalculator slotCalculator;
    private SchedulingServiceImpl schedulingService;

    private Dealership dealership;
    private ServiceType serviceType;
    private ServiceBay serviceBay;
    private Technician technician;
    private Customer customer;

    private final Long dealershipId = 1L;
    private final Long serviceTypeId = 2L;
    private final Long serviceBayId = 10L;
    private final Long technicianId = 20L;
    private final Long customerId = 1L;
    private final String vin = "1HGCR2F83HA000001";

    @BeforeEach
    void setUp() {
        slotCalculator = new SlotCalculator();
        schedulingService = new SchedulingServiceImpl(
            dealershipRepository,
            serviceTypeRepository,
            serviceBayRepository,
            technicianRepository,
            appointmentRepository,
            auditLogRepository,
            customerRepository,
            slotCalculator,
            new SimpleMeterRegistry()
        );

        customer = new Customer(customerId, "CUST-001", "John", "Doe", "john.doe@example.com", "+1-555-0101");

        dealership = new Dealership(
            dealershipId, "DLR-01", "Central Metro Dealership", "100 Main St", "Metropolis",
            LocalTime.of(8, 0), LocalTime.of(18, 0), "UTC"
        );

        serviceType = new ServiceType(
            serviceTypeId, "BRAKE_SERVICE", "Brake Pad Replacement", "Standard pad replacement",
            60, "BRAKES"
        );

        serviceBay = new ServiceBay(serviceBayId, dealership, 1, "Bay 1");
        technician = new Technician(technicianId, dealership, "TECH-01", "John", "Doe", "johndoe@dealer.com");
    }

    @Test
    @DisplayName("shouldBookAppointmentSuccessfully_whenBayAndTechnicianAreAvailable")
    void shouldBookAppointmentSuccessfully_whenBayAndTechnicianAreAvailable() {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, startTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(serviceBayRepository.findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(dealershipId))
            .thenReturn(List.of(serviceBay));
        when(technicianRepository.findActiveTechniciansWithSkill(dealershipId, "BRAKES"))
            .thenReturn(List.of(technician));
        when(serviceBayRepository.findAllByIdWithLock(anyList())).thenReturn(List.of(serviceBay));
        when(technicianRepository.findAllByIdWithLock(anyList())).thenReturn(List.of(technician));

        when(appointmentRepository.countConflictingBayAppointments(eq(serviceBayId), eq(startTime), any(), anyList()))
            .thenReturn(0L);
        when(appointmentRepository.countConflictingTechnicianAppointments(eq(technicianId), eq(startTime), any(), anyList()))
            .thenReturn(0L);

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment app = invocation.getArgument(0);
            app.setId(UUID.randomUUID());
            return app;
        });

        var response = schedulingService.bookAppointment(request);

        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.vehicleVin()).isEqualTo(vin);
        assertThat(response.serviceBayId()).isEqualTo(serviceBayId);
        assertThat(response.technicianId()).isEqualTo(technicianId);
        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(response.referenceNumber()).startsWith("APP-20261101-");

        verify(appointmentRepository).save(any(Appointment.class));
        verify(auditLogRepository).save(any(AppointmentAuditLog.class));
    }

    @Test
    @DisplayName("shouldThrowInvalidBookingException_whenRequestedTimeIsNotOn30MinuteGrid")
    void shouldThrowInvalidBookingException_whenRequestedTimeIsNotOn30MinuteGrid() {
        OffsetDateTime offGridTime = OffsetDateTime.of(2026, 11, 1, 9, 15, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, offGridTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("30-minute schedule grid");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldThrowInvalidBookingException_whenRequestedTimeIsOutsideOperatingHours")
    void shouldThrowInvalidBookingException_whenRequestedTimeIsOutsideOperatingHours() {
        OffsetDateTime lateTime = OffsetDateTime.of(2026, 11, 1, 17, 30, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, lateTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("outside dealership operating hours");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenDealershipNotFound")
    void shouldThrowResourceNotFoundException_whenDealershipNotFound() {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, startTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Dealership not found");
    }

    @Test
    @DisplayName("shouldThrowResourceUnavailableException_whenBayHasConflict")
    void shouldThrowResourceUnavailableException_whenBayHasConflict() {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, startTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(serviceBayRepository.findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(dealershipId))
            .thenReturn(List.of(serviceBay));
        when(technicianRepository.findActiveTechniciansWithSkill(dealershipId, "BRAKES"))
            .thenReturn(List.of(technician));
        when(serviceBayRepository.findAllByIdWithLock(anyList())).thenReturn(List.of(serviceBay));
        when(technicianRepository.findAllByIdWithLock(anyList())).thenReturn(List.of(technician));

        when(appointmentRepository.countConflictingBayAppointments(eq(serviceBayId), eq(startTime), any(), anyList()))
            .thenReturn(1L);

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(ResourceUnavailableException.class)
            .hasMessageContaining("No service bay or qualified technician is available");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldCancelAppointmentSuccessfully_whenStatusIsConfirmed")
    void shouldCancelAppointmentSuccessfully_whenStatusIsConfirmed() {
        UUID appId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        Appointment appointment = new Appointment(appId, "APP-20261101-ABCDEF", customer, vin,
            dealership, serviceType, serviceBay, technician, start, start.plusMinutes(60), 60);

        when(appointmentRepository.findById(appId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        var response = schedulingService.cancelAppointment(appId, new CancellationRequestDto("Customer relocated"));

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(response.cancellationReason()).isEqualTo("Customer relocated");
        verify(appointmentRepository).save(appointment);
        verify(auditLogRepository).save(any(AppointmentAuditLog.class));
    }

    @Test
    @DisplayName("shouldThrowInvalidBookingException_whenCancellingAlreadyCancelledAppointment")
    void shouldThrowInvalidBookingException_whenCancellingAlreadyCancelledAppointment() {
        UUID appId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        Appointment appointment = new Appointment(appId, "APP-20261101-ABCDEF", customer, vin,
            dealership, serviceType, serviceBay, technician, start, start.plusMinutes(60), 60);
        appointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(appId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> schedulingService.cancelAppointment(appId, new CancellationRequestDto("Duplicate")))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("Cannot cancel appointment with status CANCELLED");
    }

    @Test
    @DisplayName("shouldRescheduleAppointmentSuccessfully_whenNewSlotIsAvailable")
    void shouldRescheduleAppointmentSuccessfully_whenNewSlotIsAvailable() {
        UUID appId = UUID.randomUUID();
        OffsetDateTime originalStart = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        Appointment appointment = new Appointment(appId, "APP-20261101-ABCDEF", customer, vin,
            dealership, serviceType, serviceBay, technician, originalStart, originalStart.plusMinutes(60), 60);

        OffsetDateTime newStart = OffsetDateTime.of(2026, 11, 2, 10, 0, 0, 0, ZoneOffset.UTC);

        when(appointmentRepository.findById(appId)).thenReturn(Optional.of(appointment));
        when(serviceBayRepository.findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(dealershipId))
            .thenReturn(List.of(serviceBay));
        when(technicianRepository.findActiveTechniciansWithSkill(dealershipId, "BRAKES"))
            .thenReturn(List.of(technician));
        when(serviceBayRepository.findAllByIdWithLock(anyList())).thenReturn(List.of(serviceBay));
        when(technicianRepository.findAllByIdWithLock(anyList())).thenReturn(List.of(technician));

        when(appointmentRepository.countConflictingBayAppointmentsExcluding(eq(serviceBayId), eq(appId), eq(newStart), any(), anyList()))
            .thenReturn(0L);
        when(appointmentRepository.countConflictingTechnicianAppointmentsExcluding(eq(technicianId), eq(appId), eq(newStart), any(), anyList()))
            .thenReturn(0L);

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        var response = schedulingService.rescheduleAppointment(appId, new RescheduleRequestDto(newStart, "Shifted meeting"));

        assertThat(response.startTime()).isEqualTo(newStart);
        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentRepository).save(appointment);
        verify(auditLogRepository).save(any(AppointmentAuditLog.class));
    }

    @Test
    @DisplayName("shouldGetAppointmentById_andByReference")
    void shouldGetAppointmentById_andByReference() {
        UUID appId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        Appointment appointment = new Appointment(appId, "APP-REF-12345", customer, vin,
            dealership, serviceType, serviceBay, technician, start, start.plusMinutes(60), 60);

        when(appointmentRepository.findById(appId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.findByReferenceNumber("APP-REF-12345")).thenReturn(Optional.of(appointment));

        var byId = schedulingService.getAppointmentById(appId);
        var byRef = schedulingService.getAppointmentByReference("APP-REF-12345");

        assertThat(byId.id()).isEqualTo(appId);
        assertThat(byRef.referenceNumber()).isEqualTo("APP-REF-12345");
    }

    @Test
    @DisplayName("shouldReturnEmptySlots_whenDealershipHasZeroBays")
    void shouldReturnEmptySlots_whenDealershipHasZeroBays() {
        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(serviceBayRepository.findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(dealershipId))
            .thenReturn(List.of());
        when(technicianRepository.findActiveTechniciansWithSkill(dealershipId, "BRAKES"))
            .thenReturn(List.of(technician));

        var schedule = schedulingService.getAvailableSlots(dealershipId, serviceTypeId, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), null);
        assertThat(schedule.totalAvailableSlots()).isEqualTo(0);
        assertThat(schedule.days().getFirst().slots()).isEmpty();
    }

    @Test
    @DisplayName("shouldThrowInvalidBookingException_whenDealershipIsInactive")
    void shouldThrowInvalidBookingException_whenDealershipIsInactive() {
        dealership.setActive(false);
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, startTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("Dealership is inactive");

        assertThatThrownBy(() -> schedulingService.getAvailableSlots(dealershipId, serviceTypeId, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), null))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("Dealership is inactive");
    }

    @Test
    @DisplayName("shouldThrowInvalidBookingException_whenServiceTypeIsInactive")
    void shouldThrowInvalidBookingException_whenServiceTypeIsInactive() {
        serviceType.setActive(false);
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, startTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("Service type is inactive");

        assertThatThrownBy(() -> schedulingService.getAvailableSlots(dealershipId, serviceTypeId, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), null))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("Service type is inactive");
    }

    @Test
    @DisplayName("shouldThrowInvalidBookingException_whenDateRangeExceeds31Days")
    void shouldThrowInvalidBookingException_whenDateRangeExceeds31Days() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(40);

        assertThatThrownBy(() -> schedulingService.getAvailableSlots(dealershipId, serviceTypeId, start, end, null))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("cannot exceed 31 days");
    }

    @Test
    @DisplayName("shouldFilterAvailableSlots_byDesiredTime")
    void shouldFilterAvailableSlots_byDesiredTime() {
        LocalDate targetDate = LocalDate.now().plusDays(2);
        LocalTime desiredTime = LocalTime.of(9, 0);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(serviceBayRepository.findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(dealershipId))
            .thenReturn(List.of(serviceBay));
        when(technicianRepository.findActiveTechniciansWithSkill(dealershipId, "BRAKES"))
            .thenReturn(List.of(technician));
        when(appointmentRepository.findActiveAppointmentsInWindow(eq(dealershipId), any(), any(), any()))
            .thenReturn(List.of());

        var schedule = schedulingService.getAvailableSlots(dealershipId, serviceTypeId, targetDate, targetDate, desiredTime);

        assertThat(schedule.totalAvailableSlots()).isEqualTo(1);
        var slot = schedule.days().getFirst().slots().getFirst();
        assertThat(slot.startTime().toLocalTime()).isEqualTo(desiredTime);
        assertThat(slot.availableCapacity()).isGreaterThan(0);
        assertThat(slot.availableTechnicians()).hasSize(1);
        assertThat(slot.availableTechnicians().getFirst().name()).isEqualTo(technician.getFullName());
        assertThat(slot.availableBays()).hasSize(1);
        assertThat(slot.availableBays().getFirst().name()).isEqualTo(serviceBay.getName());
    }

    @Test
    @DisplayName("shouldSearchAppointments_byVinAndDealershipStatusAndAll")
    void shouldSearchAppointments_byVinAndDealershipStatusAndAll() {
        UUID appId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        Appointment appointment = new Appointment(appId, "APP-REF-1", customer, vin,
            dealership, serviceType, serviceBay, technician, start, start.plusMinutes(60), 60);

        when(appointmentRepository.findByVehicleVinOrderByStartTimeDesc(vin)).thenReturn(List.of(appointment));
        when(appointmentRepository.findByDealershipIdAndStatusOrderByStartTimeDesc(dealershipId, AppointmentStatus.CONFIRMED))
            .thenReturn(List.of(appointment));
        when(appointmentRepository.findAll()).thenReturn(List.of(appointment));

        var byVin = schedulingService.searchAppointments(null, vin, null, null);
        assertThat(byVin).hasSize(1);

        var byDealerStatus = schedulingService.searchAppointments(null, null, dealershipId, AppointmentStatus.CONFIRMED);
        assertThat(byDealerStatus).hasSize(1);

        var all = schedulingService.searchAppointments(null, null, null, null);
        assertThat(all).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("shouldSearchAppointmentsPaged_withPagination")
    void shouldSearchAppointmentsPaged_withPagination() {
        UUID appId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        Appointment appointment = new Appointment(appId, "APP-REF-1", customer, vin,
            dealership, serviceType, serviceBay, technician, start, start.plusMinutes(60), 60);

        var pageRequest = PageRequest.of(0, 10);
        when(appointmentRepository.findAll(any(Specification.class), eq(pageRequest)))
            .thenReturn(new PageImpl<>(List.of(appointment), pageRequest, 1));

        var paged = schedulingService.searchAppointmentsPaged("APP-REF", dealershipId, null, null, null, pageRequest);
        assertThat(paged.content()).hasSize(1);
        assertThat(paged.totalElements()).isEqualTo(1);
        assertThat(paged.pageNumber()).isEqualTo(0);
        assertThat(paged.isFirst()).isTrue();
    }

    @Test
    @DisplayName("shouldThrowInvalidBookingException_whenSearchingAppointments_withTechnicianNotBelongingToDealership")
    void shouldThrowInvalidBookingException_whenSearchingAppointments_withTechnicianNotBelongingToDealership() {
        Dealership otherDealership = new Dealership(99L, "DLR-OTHER", "Other Dealer", "123 Main St", "Other City",
            LocalTime.of(8, 0), LocalTime.of(18, 0), "America/New_York");
        Technician otherTech = new Technician(10L, otherDealership, "TECH-OTHER", "John", "Doe", "john@example.com");

        when(technicianRepository.findById(10L)).thenReturn(Optional.of(otherTech));

        var pageRequest = PageRequest.of(0, 10);
        assertThatThrownBy(() -> schedulingService.searchAppointmentsPaged(null, dealershipId, null, 10L, null, pageRequest))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("does not belong to dealership with ID " + dealershipId);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenSearchingAppointments_withNonExistentTechnician")
    void shouldThrowResourceNotFoundException_whenSearchingAppointments_withNonExistentTechnician() {
        when(technicianRepository.findById(999L)).thenReturn(Optional.empty());

        var pageRequest = PageRequest.of(0, 10);
        assertThatThrownBy(() -> schedulingService.searchAppointmentsPaged(null, dealershipId, null, 999L, null, pageRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Technician not found with id: 999");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenCustomerDoesNotExist")
    void shouldThrowResourceNotFoundException_whenCustomerDoesNotExist() {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, startTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Customer not found with ID");
    }

    @Test
    @DisplayName("shouldThrowInvalidBookingException_whenCustomerIsInactive")
    void shouldThrowInvalidBookingException_whenCustomerIsInactive() {
        customer.setActive(false);
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, startTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("Customer account is inactive");
    }

    @Test
    @DisplayName("shouldThrowResourceUnavailableException_whenCustomerHasOverlappingAppointment")
    void shouldThrowResourceUnavailableException_whenCustomerHasOverlappingAppointment() {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, startTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(appointmentRepository.countConflictingCustomerAppointments(eq(customerId), any(), any(), any()))
            .thenReturn(1L);

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(ResourceUnavailableException.class)
            .hasMessageContaining("already has an active appointment scheduled during slot");
    }

    @Test
    @DisplayName("shouldThrowResourceUnavailableException_whenVehicleVinHasOverlappingAppointment")
    void shouldThrowResourceUnavailableException_whenVehicleVinHasOverlappingAppointment() {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(customerId, vin, dealershipId, serviceTypeId, startTime);

        when(dealershipRepository.findById(dealershipId)).thenReturn(Optional.of(dealership));
        when(serviceTypeRepository.findById(serviceTypeId)).thenReturn(Optional.of(serviceType));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(appointmentRepository.countConflictingCustomerAppointments(eq(customerId), any(), any(), any()))
            .thenReturn(0L);
        when(appointmentRepository.countConflictingVehicleAppointments(eq(vin), any(), any(), any()))
            .thenReturn(1L);

        assertThatThrownBy(() -> schedulingService.bookAppointment(request))
            .isInstanceOf(ResourceUnavailableException.class)
            .hasMessageContaining("Vehicle with VIN " + vin + " already has an active appointment scheduled during slot");
    }

    @Test
    @DisplayName("shouldReturnAuditLogs_whenAppointmentExists")
    void shouldReturnAuditLogs_whenAppointmentExists() {
        UUID appointmentId = UUID.randomUUID();
        var appointment = new Appointment();
        appointment.setId(appointmentId);

        var log1 = new AppointmentAuditLog(appointmentId, "BOOKED", null, "CONFIRMED", "Initial booking", "SYSTEM");
        log1.setId(UUID.randomUUID());
        var log2 = new AppointmentAuditLog(appointmentId, "RESCHEDULED", "CONFIRMED", "CONFIRMED", "Rescheduled slot", "SYSTEM");
        log2.setId(UUID.randomUUID());

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(auditLogRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId)).thenReturn(List.of(log2, log1));

        var logs = schedulingService.getAppointmentAuditLogs(appointmentId);

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).action()).isEqualTo("RESCHEDULED");
        assertThat(logs.get(1).action()).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenGettingAuditLogsForNonExistentAppointment")
    void shouldThrowResourceNotFoundException_whenGettingAuditLogsForNonExistentAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService.getAppointmentAuditLogs(appointmentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Appointment not found with ID: " + appointmentId);
    }
}
