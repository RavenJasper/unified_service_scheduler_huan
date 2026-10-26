package com.unified.scheduler.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    @DisplayName("shouldExerciseDealershipGettersSettersAndLifecycle")
    void shouldExerciseDealershipGettersSettersAndLifecycle() {
        Dealership d = new Dealership();
        Long id = 1L;
        d.setId(id);
        d.setCode("DLR-TEST");
        d.setName("Test Dealer");
        d.setAddress("456 Test Blvd");
        d.setCity("Test City");
        d.setOperatingOpenTime(LocalTime.of(7, 30));
        d.setOperatingCloseTime(LocalTime.of(19, 0));
        d.setTimeZone("America/New_York");
        d.setActive(false);
        d.setVersion(1);

        OffsetDateTime now = OffsetDateTime.now();
        d.setCreatedAt(now);
        d.setUpdatedAt(now);

        assertThat(d.getId()).isEqualTo(id);
        assertThat(d.getCode()).isEqualTo("DLR-TEST");
        assertThat(d.getName()).isEqualTo("Test Dealer");
        assertThat(d.getAddress()).isEqualTo("456 Test Blvd");
        assertThat(d.getCity()).isEqualTo("Test City");
        assertThat(d.getOperatingOpenTime()).isEqualTo(LocalTime.of(7, 30));
        assertThat(d.getOperatingCloseTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(d.getTimeZone()).isEqualTo("America/New_York");
        assertThat(d.isActive()).isFalse();
        assertThat(d.getVersion()).isEqualTo(1);
        assertThat(d.getCreatedAt()).isEqualTo(now);
        assertThat(d.getUpdatedAt()).isEqualTo(now);

        // PrePersist & PreUpdate
        Dealership d2 = new Dealership();
        d2.onCreate();
        assertThat(d2.getCreatedAt()).isNotNull();
        assertThat(d2.getUpdatedAt()).isNotNull();
        d2.onUpdate();
        assertThat(d2.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("shouldExerciseServiceTypeGettersSettersAndLifecycle")
    void shouldExerciseServiceTypeGettersSettersAndLifecycle() {
        ServiceType st = new ServiceType();
        Long id = 2L;
        st.setId(id);
        st.setCode("TIRE_ROTATION");
        st.setName("Tire Rotation");
        st.setDescription("Rotate 4 tires");
        st.setStandardDurationMinutes(45);
        st.setRequiredSkillCode("TIRES");
        st.setActive(true);
        st.setVersion(2);

        OffsetDateTime now = OffsetDateTime.now();
        st.setCreatedAt(now);
        st.setUpdatedAt(now);

        assertThat(st.getId()).isEqualTo(id);
        assertThat(st.getCode()).isEqualTo("TIRE_ROTATION");
        assertThat(st.getName()).isEqualTo("Tire Rotation");
        assertThat(st.getDescription()).isEqualTo("Rotate 4 tires");
        assertThat(st.getStandardDurationMinutes()).isEqualTo(45);
        assertThat(st.getRequiredSkillCode()).isEqualTo("TIRES");
        assertThat(st.isActive()).isTrue();
        assertThat(st.getVersion()).isEqualTo(2);
        assertThat(st.getCreatedAt()).isEqualTo(now);
        assertThat(st.getUpdatedAt()).isEqualTo(now);

        ServiceType st2 = new ServiceType();
        st2.onCreate();
        assertThat(st2.getCreatedAt()).isNotNull();
        st2.onUpdate();
        assertThat(st2.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("shouldExerciseServiceBayGettersSettersAndLifecycle")
    void shouldExerciseServiceBayGettersSettersAndLifecycle() {
        ServiceBay bay = new ServiceBay();
        Long id = 3L;
        Dealership d = new Dealership();
        bay.setId(id);
        bay.setDealership(d);
        bay.setBayNumber(3);
        bay.setName("Bay 3");
        bay.setOperational(false);
        bay.setVersion(1);

        OffsetDateTime now = OffsetDateTime.now();
        bay.setCreatedAt(now);
        bay.setUpdatedAt(now);

        assertThat(bay.getId()).isEqualTo(id);
        assertThat(bay.getDealership()).isEqualTo(d);
        assertThat(bay.getBayNumber()).isEqualTo(3);
        assertThat(bay.getName()).isEqualTo("Bay 3");
        assertThat(bay.isOperational()).isFalse();
        assertThat(bay.getVersion()).isEqualTo(1);
        assertThat(bay.getCreatedAt()).isEqualTo(now);
        assertThat(bay.getUpdatedAt()).isEqualTo(now);

        ServiceBay bay2 = new ServiceBay();
        bay2.onCreate();
        assertThat(bay2.getCreatedAt()).isNotNull();
        bay2.onUpdate();
        assertThat(bay2.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("shouldExerciseTechnicianAndTechnicianSkill")
    void shouldExerciseTechnicianAndTechnicianSkill() {
        Technician tech = new Technician();
        Long id = 4L;
        Dealership d = new Dealership();
        tech.setId(id);
        tech.setDealership(d);
        tech.setEmployeeCode("EMP-100");
        tech.setFirstName("Alice");
        tech.setLastName("Wonder");
        tech.setEmail("alice@wonder.com");
        tech.setActive(true);
        tech.setVersion(1);

        OffsetDateTime now = OffsetDateTime.now();
        tech.setCreatedAt(now);
        tech.setUpdatedAt(now);

        assertThat(tech.getId()).isEqualTo(id);
        assertThat(tech.getDealership()).isEqualTo(d);
        assertThat(tech.getEmployeeCode()).isEqualTo("EMP-100");
        assertThat(tech.getFirstName()).isEqualTo("Alice");
        assertThat(tech.getLastName()).isEqualTo("Wonder");
        assertThat(tech.getFullName()).isEqualTo("Alice Wonder");
        assertThat(tech.getEmail()).isEqualTo("alice@wonder.com");
        assertThat(tech.isActive()).isTrue();
        assertThat(tech.getVersion()).isEqualTo(1);
        assertThat(tech.getCreatedAt()).isEqualTo(now);
        assertThat(tech.getUpdatedAt()).isEqualTo(now);

        TechnicianSkill skill = new TechnicianSkill();
        Long skillId = 5L;
        skill.setId(skillId);
        skill.setTechnician(tech);
        skill.setSkillCode("TRANSMISSION");
        skill.setCertifiedDate(LocalDate.of(2023, 1, 1));
        skill.setCreatedAt(now);

        assertThat(skill.getId()).isEqualTo(skillId);
        assertThat(skill.getTechnician()).isEqualTo(tech);
        assertThat(skill.getSkillCode()).isEqualTo("TRANSMISSION");
        assertThat(skill.getCertifiedDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(skill.getCreatedAt()).isEqualTo(now);

        tech.addSkill(skill);
        assertThat(tech.getSkills()).hasSize(1);
        assertThat(tech.hasSkill("TRANSMISSION")).isTrue();
        assertThat(tech.hasSkill("BRAKES")).isFalse();
        assertThat(tech.hasSkill(null)).isFalse();

        tech.removeSkill(skill);
        assertThat(tech.getSkills()).isEmpty();

        Technician tech2 = new Technician();
        tech2.onCreate();
        assertThat(tech2.getCreatedAt()).isNotNull();
        tech2.onUpdate();
        assertThat(tech2.getUpdatedAt()).isNotNull();

        TechnicianSkill skill2 = new TechnicianSkill();
        skill2.onCreate();
        assertThat(skill2.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("shouldExerciseAppointmentAndAuditLog")
    void shouldExerciseAppointmentAndAuditLog() {
        Appointment app = new Appointment();
        UUID id = UUID.randomUUID();
        Long custId = 1L;
        Customer cust = new Customer(custId, "CUST-001", "John", "Doe", "john@example.com", "555-1234");
        Dealership d = new Dealership();
        ServiceType st = new ServiceType();
        ServiceBay sb = new ServiceBay();
        Technician tech = new Technician();
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(60);

        app.setId(id);
        app.setReferenceNumber("APP-999");
        app.setCustomer(cust);
        app.setVehicleVin("1HGCR2F83HA000123");
        app.setDealership(d);
        app.setServiceType(st);
        app.setServiceBay(sb);
        app.setTechnician(tech);
        app.setStartTime(start);
        app.setEndTime(end);
        app.setDurationMinutes(60);
        app.setStatus(AppointmentStatus.CONFIRMED);
        app.setCancellationReason(null);
        app.setVersion(0);
        app.setCreatedAt(start);
        app.setUpdatedAt(start);

        assertThat(app.getId()).isEqualTo(id);
        assertThat(app.getReferenceNumber()).isEqualTo("APP-999");
        assertThat(app.getCustomerId()).isEqualTo(custId);
        assertThat(app.getVehicleVin()).isEqualTo("1HGCR2F83HA000123");
        assertThat(app.getDealership()).isEqualTo(d);
        assertThat(app.getServiceType()).isEqualTo(st);
        assertThat(app.getServiceBay()).isEqualTo(sb);
        assertThat(app.getTechnician()).isEqualTo(tech);
        assertThat(app.getStartTime()).isEqualTo(start);
        assertThat(app.getEndTime()).isEqualTo(end);
        assertThat(app.getDurationMinutes()).isEqualTo(60);
        assertThat(app.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(app.isActive()).isTrue();
        assertThat(app.getCancellationReason()).isNull();
        assertThat(app.getVersion()).isEqualTo(0);
        assertThat(app.getCreatedAt()).isEqualTo(start);
        assertThat(app.getUpdatedAt()).isEqualTo(start);

        Appointment app2 = new Appointment();
        app2.onCreate();
        assertThat(app2.getCreatedAt()).isNotNull();
        app2.onUpdate();
        assertThat(app2.getUpdatedAt()).isNotNull();

        // AppointmentAuditLog
        AppointmentAuditLog log = new AppointmentAuditLog();
        UUID logId = UUID.randomUUID();
        log.setId(logId);
        log.setAppointmentId(id);
        log.setAction("UPDATED");
        log.setStatusFrom("CONFIRMED");
        log.setStatusTo("COMPLETED");
        log.setReason("Job finished");
        log.setPerformedBy("MANAGER");
        log.setCreatedAt(start);

        assertThat(log.getId()).isEqualTo(logId);
        assertThat(log.getAppointmentId()).isEqualTo(id);
        assertThat(log.getAction()).isEqualTo("UPDATED");
        assertThat(log.getStatusFrom()).isEqualTo("CONFIRMED");
        assertThat(log.getStatusTo()).isEqualTo("COMPLETED");
        assertThat(log.getReason()).isEqualTo("Job finished");
        assertThat(log.getPerformedBy()).isEqualTo("MANAGER");
        assertThat(log.getCreatedAt()).isEqualTo(start);

        AppointmentAuditLog log2 = new AppointmentAuditLog();
        log2.onCreate();
        assertThat(log2.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("shouldExerciseCustomerEntityGettersAndSetters")
    void shouldExerciseCustomerEntityGettersAndSetters() {
        Long id = 1L;
        OffsetDateTime now = OffsetDateTime.now();
        Customer customer = new Customer(id, "CUST-001", "John", "Doe", "john@example.com", "555-1234", true, 0, now, now);

        assertThat(customer.getId()).isEqualTo(id);
        assertThat(customer.getCustomerCode()).isEqualTo("CUST-001");
        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getFullName()).isEqualTo("John Doe");
        assertThat(customer.getEmail()).isEqualTo("john@example.com");
        assertThat(customer.getPhone()).isEqualTo("555-1234");
        assertThat(customer.isActive()).isTrue();
        assertThat(customer.getVersion()).isEqualTo(0);
        assertThat(customer.getCreatedAt()).isEqualTo(now);
        assertThat(customer.getUpdatedAt()).isEqualTo(now);

        customer.setCustomerCode("CUST-999");
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setEmail("jane@example.com");
        customer.setPhone("555-5678");
        customer.setActive(false);
        customer.setVersion(1);

        assertThat(customer.getCustomerCode()).isEqualTo("CUST-999");
        assertThat(customer.getFullName()).isEqualTo("Jane Smith");
        assertThat(customer.getEmail()).isEqualTo("jane@example.com");
        assertThat(customer.getPhone()).isEqualTo("555-5678");
        assertThat(customer.isActive()).isFalse();

        Customer customer2 = new Customer();
        customer2.setId(id);
        customer2.onCreate();
        assertThat(customer2.getCreatedAt()).isNotNull();
        customer2.onUpdate();
        assertThat(customer2.getUpdatedAt()).isNotNull();

        assertThat(customer).isEqualTo(customer2);
        assertThat(customer.hashCode()).isEqualTo(customer2.hashCode());
    }
}
