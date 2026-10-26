package com.unified.scheduler.service;

import com.unified.scheduler.domain.*;
import com.unified.scheduler.dto.BookingRequestDto;
import com.unified.scheduler.exception.ResourceUnavailableException;
import com.unified.scheduler.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AppointmentConcurrencyTest {

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private DealershipRepository dealershipRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private ServiceBayRepository serviceBayRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Dealership dealership;
    private ServiceType serviceType;
    private ServiceBay bay;
    private Technician technician;
    private Customer customer;

    @BeforeEach
    void setUp() {
        // Create an isolated dealership with exactly 1 bay and 1 technician to create extreme contention
        dealership = new Dealership(
            null,
            "DLR-CONC-" + UUID.randomUUID().toString().substring(0, 6),
            "Concurrency Test Motors",
            "500 Race Track Rd",
            "Speedway",
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
            "UTC"
        );
        dealership = dealershipRepository.save(dealership);

        serviceType = new ServiceType(
            null,
            "SRV-CONC-" + UUID.randomUUID().toString().substring(0, 6),
            "Concurrency Oil Flush",
            "Single bay test service",
            60,
            "SKILL_CONC"
        );
        serviceType = serviceTypeRepository.save(serviceType);

        bay = new ServiceBay(null, dealership, 1, "Exclusive Bay 1");
        bay = serviceBayRepository.save(bay);

        technician = new Technician(
            null,
            dealership,
            "TECH-CONC-" + UUID.randomUUID().toString().substring(0, 6),
            "Race",
            "Driver",
            "driver@speedway.com"
        );
        technician.addSkill(new TechnicianSkill(null, technician, "SKILL_CONC", LocalDate.now()));
        technician = technicianRepository.save(technician);

        customer = new Customer(null, "CUST-CONC-" + UUID.randomUUID().toString().substring(0, 6), "Conc", "Tester", "conc-" + UUID.randomUUID() + "@speedway.com", "555-9999");
        customer = customerRepository.save(customer);
    }

    @Test
    @DisplayName("shouldEnsureZeroDoubleBooking_whenMultipleThreadsCompeteForExactSameSlot")
    void shouldEnsureZeroDoubleBooking_whenMultipleThreadsCompeteForExactSameSlot() throws InterruptedException {
        // Arrange
        int concurrentThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(concurrentThreads);

        // Create 20 unique customers so threads compete strictly for the single bay & technician
        List<Customer> testCustomers = new ArrayList<>();
        for (int i = 0; i < concurrentThreads; i++) {
            Customer c = new Customer(null, "CUST-CONC-" + i + "-" + UUID.randomUUID().toString().substring(0, 4),
                "Conc" + i, "Tester", "conc" + i + "-" + UUID.randomUUID() + "@speedway.com", "555-9999");
            testCustomers.add(customerRepository.save(c));
        }

        OffsetDateTime targetSlot = OffsetDateTime.of(2026, 12, 1, 10, 0, 0, 0, ZoneOffset.UTC);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);

        // Act: Dispatch 20 concurrent threads
        for (int i = 0; i < concurrentThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGun.await(); // Synchronize all threads to fire simultaneously

                    var request = new BookingRequestDto(
                        testCustomers.get(index).getId(),
                        String.format("1HGCR2F83HA000%03d", index),
                        dealership.getId(),
                        serviceType.getId(),
                        targetSlot
                    );

                    schedulingService.bookAppointment(request);
                    successCount.incrementAndGet();
                } catch (ResourceUnavailableException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    otherErrors.incrementAndGet();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Fire the starting gun!
        startGun.countDown();

        // Wait for all 20 threads to complete within 30 seconds
        boolean completed = doneSignal.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertThat(completed).as("All concurrency threads should complete in time").isTrue();
        assertThat(otherErrors.get()).as("There should be no unexpected errors").isEqualTo(0);

        // Keyloop Critical Requirement: Zero Double-Booking Guarantee!
        assertThat(successCount.get())
            .as("Exactly ONE thread must succeed in booking the contested slot")
            .isEqualTo(1);

        assertThat(conflictCount.get())
            .as("All remaining 19 competing threads must receive 409 Conflict rejection")
            .isEqualTo(concurrentThreads - 1);

        // Verify Database Invariant: Exactly 1 record in appointments table for this slot
        long bayAppointments = appointmentRepository.countConflictingBayAppointments(
            bay.getId(),
            targetSlot,
            targetSlot.plusMinutes(serviceType.getStandardDurationMinutes()),
            java.util.List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)
        );
        assertThat(bayAppointments).as("Database must contain exactly 1 appointment for the service bay").isEqualTo(1);
    }
}
