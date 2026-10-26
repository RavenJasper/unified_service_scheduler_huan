package com.unified.scheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unified.scheduler.domain.Dealership;
import com.unified.scheduler.domain.ServiceType;
import com.unified.scheduler.domain.Technician;
import com.unified.scheduler.dto.BookingRequestDto;
import com.unified.scheduler.dto.CancellationRequestDto;
import com.unified.scheduler.dto.ReassignTechnicianRequestDto;
import com.unified.scheduler.dto.RescheduleRequestDto;
import com.unified.scheduler.repository.DealershipRepository;
import com.unified.scheduler.repository.ServiceTypeRepository;
import com.unified.scheduler.repository.TechnicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DealershipRepository dealershipRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    private Long centralMetroId;
    private Long brakeServiceId;
    private Long tech1Id;
    private Long tech2Id;
    private Long tech4Id;

    @BeforeEach
    void setUp() {
        centralMetroId = dealershipRepository.findByCode("DLR-METRO")
            .map(Dealership::getId)
            .orElse(1L);
        brakeServiceId = serviceTypeRepository.findByCode("BRAKE_SERVICE")
            .map(ServiceType::getId)
            .orElse(2L);
        tech1Id = technicianRepository.findAll().stream()
            .filter(t -> "TECH-001".equals(t.getEmployeeCode()))
            .findFirst().map(Technician::getId).orElse(1L);
        tech2Id = technicianRepository.findAll().stream()
            .filter(t -> "TECH-002".equals(t.getEmployeeCode()))
            .findFirst().map(Technician::getId).orElse(2L);
        tech4Id = technicianRepository.findAll().stream()
            .filter(t -> "TECH-004".equals(t.getEmployeeCode()))
            .findFirst().map(Technician::getId).orElse(4L);
    }

    @Test
    @DisplayName("shouldReturnDealerships_whenListingCatalog")
    void shouldReturnDealerships_whenListingCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/dealerships/lookup")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(2))))
            .andExpect(jsonPath("$.items[0].code", notNullValue()))
            .andExpect(jsonPath("$.items[0].label", notNullValue()))
            .andExpect(jsonPath("$.hasMore", is(false)));

        mockMvc.perform(get("/api/v1/dealerships")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("shouldReturnServiceTypes_whenListingCatalog")
    void shouldReturnServiceTypes_whenListingCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/service-types/lookup")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(4))))
            .andExpect(jsonPath("$.items[0].code", notNullValue()))
            .andExpect(jsonPath("$.items[0].label", notNullValue()));
    }

    @Test
    @DisplayName("shouldReturnTechniciansWithSkills_whenListingCatalog")
    void shouldReturnTechniciansWithSkills_whenListingCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/technicians/lookup")
                .param("dealershipId", centralMetroId.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.items[0].code", notNullValue()))
            .andExpect(jsonPath("$.items[0].label", notNullValue()));
    }

    @Test
    @DisplayName("shouldReturnServiceBays_whenListingCatalog")
    void shouldReturnServiceBays_whenListingCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/service-bays/lookup")
                .param("dealershipId", centralMetroId.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.items[0].code", notNullValue()))
            .andExpect(jsonPath("$.items[0].label", notNullValue()));
    }

    @Test
    @DisplayName("shouldReturnAvailableSlots_whenQueryingDealershipAndDate")
    void shouldReturnAvailableSlots_whenQueryingDealershipAndDate() throws Exception {
        mockMvc.perform(get("/api/v1/schedules/available-slots")
                .param("dealershipId", centralMetroId.toString())
                .param("serviceTypeId", brakeServiceId.toString())
                .param("date", "2026-11-10")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dealershipId", is(centralMetroId.intValue())))
            .andExpect(jsonPath("$.serviceDurationMinutes", is(60)))
            .andExpect(jsonPath("$.totalAvailableSlots", greaterThan(0)))
            .andExpect(jsonPath("$.days", hasSize(1)))
            .andExpect(jsonPath("$.days[0].slots", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.days[0].slots[0].availableCapacity", greaterThan(0)))
            .andExpect(jsonPath("$.days[0].slots[0].formattedTime", notNullValue()))
            .andExpect(jsonPath("$.days[0].slots[0].availableTechnicians", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.days[0].slots[0].availableTechnicians[0].name", notNullValue()))
            .andExpect(jsonPath("$.days[0].slots[0].availableTechnicians[0].employeeCode", notNullValue()))
            .andExpect(jsonPath("$.days[0].slots[0].availableBays", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.days[0].slots[0].availableBays[0].bayNumber", notNullValue()))
            .andExpect(jsonPath("$.days[0].slots[0].availableBays[0].name", notNullValue()));
    }

    @Test
    @DisplayName("shouldReturnDefaultOneWeekSlots_whenDateIsOmitted")
    void shouldReturnDefaultOneWeekSlots_whenDateIsOmitted() throws Exception {
        mockMvc.perform(get("/api/v1/schedules/available-slots")
                .param("dealershipId", centralMetroId.toString())
                .param("serviceTypeId", brakeServiceId.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days", hasSize(7)))
            .andExpect(jsonPath("$.totalAvailableSlots", greaterThan(0)));
    }

    @Test
    @DisplayName("shouldFilterAvailableSlots_whenDesiredTimeProvided")
    void shouldFilterAvailableSlots_whenDesiredTimeProvided() throws Exception {
        mockMvc.perform(get("/api/v1/schedules/available-slots")
                .param("dealershipId", centralMetroId.toString())
                .param("serviceTypeId", brakeServiceId.toString())
                .param("date", "2026-11-10")
                .param("desiredTime", "09:00:00")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAvailableSlots", is(1)))
            .andExpect(jsonPath("$.days[0].slots[0].startTime", containsString("T09:00:00")))
            .andExpect(jsonPath("$.days[0].slots[0].formattedTime", is("09:00 - 10:00")));
    }

    @Test
    @DisplayName("shouldReturn400BadRequest_whenMissingRequiredDealershipId")
    void shouldReturn400BadRequest_whenMissingRequiredDealershipId() throws Exception {
        mockMvc.perform(get("/api/v1/schedules/available-slots")
                .param("serviceTypeId", brakeServiceId.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type", is("urn:problem-type:missing-parameter")))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.detail", containsString("dealershipId")));
    }

    @Test
    @DisplayName("shouldBookAppointment_andReturn201Created")
    void shouldBookAppointment_andReturn201Created() throws Exception {
        // Arrange
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(
            1L,
            "1HGCR2F83HA000001",
            centralMetroId,
            brakeServiceId,
            startTime
        );

        // Act & Assert
        String responseContent = mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().exists("X-Trace-Id"))
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.referenceNumber", startsWith("APP-20261110-")))
            .andExpect(jsonPath("$.status", is("CONFIRMED")))
            .andExpect(jsonPath("$.vehicleVin", is("1HGCR2F83HA000001")))
            .andReturn().getResponse().getContentAsString();

        // Extract ID and verify cancel endpoint
        String appointmentId = objectMapper.readTree(responseContent).get("id").asText();

        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", appointmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CancellationRequestDto("Customer changed plans"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("CANCELLED")))
            .andExpect(jsonPath("$.cancellationReason", is("Customer changed plans")));
    }

    @Test
    @DisplayName("shouldCancelAppointmentSuccessfully_whenPayloadUsesCancellationReasonAlias")
    void shouldCancelAppointmentSuccessfully_whenPayloadUsesCancellationReasonAlias() throws Exception {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 10, 11, 0, 0, 0, ZoneOffset.UTC);
        var bookingRequest = new BookingRequestDto(
            1L,
            "1HGCR2F83HA000001",
            centralMetroId,
            brakeServiceId,
            startTime
        );

        String responseContent = mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String appointmentId = objectMapper.readTree(responseContent).get("id").asText();

        String cancelJson = """
            {
                "cancellationReason": "Customer requested change of plans"
            }
            """;

        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", appointmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cancelJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("CANCELLED")))
            .andExpect(jsonPath("$.cancellationReason", is("Customer requested change of plans")));
    }

    @Test
    @DisplayName("shouldReturn400BadRequestWithProblemDetail_whenValidationFails")
    void shouldReturn400BadRequestWithProblemDetail_whenValidationFails() throws Exception {
        // Invalid VIN: too short
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        var invalidRequest = new BookingRequestDto(
            1L,
            "SHORT_VIN",
            centralMetroId,
            brakeServiceId,
            startTime
        );

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title", is("Validation Failure")))
            .andExpect(jsonPath("$.type", is("urn:problem-type:validation-error")))
            .andExpect(jsonPath("$.invalid_params.vehicleVin", notNullValue()))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    @DisplayName("shouldReturn400BadRequestWithProblemDetail_whenCustomerIdIsMalformedOrNonNumeric")
    void shouldReturn400BadRequestWithProblemDetail_whenCustomerIdIsMalformedOrNonNumeric() throws Exception {
        String malformedJson = """
            {
              "customerId": "33333333-3333-3333-3333-333333333333",
              "vehicleVin": "1HGCR2F83HA000001",
              "dealershipId": %d,
              "serviceTypeId": %d,
              "requestedStartTime": "2026-11-10T09:00:00Z"
            }
            """.formatted(centralMetroId, brakeServiceId);

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title", is("Malformed Request Payload")))
            .andExpect(jsonPath("$.type", is("urn:problem-type:malformed-request")))
            .andExpect(jsonPath("$.detail", containsString("Invalid value '33333333-3333-3333-3333-333333333333' for field 'customerId'")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    @DisplayName("shouldReturn404NotFoundWithProblemDetail_whenCustomerDoesNotExist")
    void shouldReturn404NotFoundWithProblemDetail_whenCustomerDoesNotExist() throws Exception {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(
            999999L,
            "1HGCR2F83HA000001",
            centralMetroId,
            brakeServiceId,
            startTime
        );

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title", is("Resource Not Found")))
            .andExpect(jsonPath("$.type", is("urn:problem-type:resource-not-found")))
            .andExpect(jsonPath("$.detail", containsString("Customer not found with ID: 999999")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    @DisplayName("shouldReturn404NotFoundWithProblemDetail_whenAppointmentDoesNotExist")
    void shouldReturn404NotFoundWithProblemDetail_whenAppointmentDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/appointments/{id}", nonExistentId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title", is("Resource Not Found")))
            .andExpect(jsonPath("$.type", is("urn:problem-type:resource-not-found")))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    @DisplayName("shouldGetDealershipById_whenExists")
    void shouldGetDealershipById_whenExists() throws Exception {
        mockMvc.perform(get("/api/v1/dealerships/{id}", centralMetroId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(centralMetroId.intValue())))
            .andExpect(jsonPath("$.code", is("DLR-METRO")));
    }

    @Test
    @DisplayName("shouldGetServiceTypeById_whenExists")
    void shouldGetServiceTypeById_whenExists() throws Exception {
        mockMvc.perform(get("/api/v1/service-types/{id}", brakeServiceId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(brakeServiceId.intValue())))
            .andExpect(jsonPath("$.code", is("BRAKE_SERVICE")));
    }

    @Test
    @DisplayName("shouldGetTechnicianById_whenExists")
    void shouldGetTechnicianById_whenExists() throws Exception {
        mockMvc.perform(get("/api/v1/technicians/{id}", 1L)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.employeeCode", notNullValue()))
            .andExpect(jsonPath("$.skills", notNullValue()));
    }

    @Test
    @DisplayName("shouldGetServiceBayById_whenExists")
    void shouldGetServiceBayById_whenExists() throws Exception {
        mockMvc.perform(get("/api/v1/service-bays/{id}", 1L)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.name", notNullValue()));
    }

    @Test
    @DisplayName("shouldSearchAppointments_andRescheduleSuccessfully")
    void shouldSearchAppointments_andRescheduleSuccessfully() throws Exception {
        Long customerId = 2L;
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(
            customerId,
            "1HGCR2F83HA000999",
            centralMetroId,
            brakeServiceId,
            startTime
        );

        String responseJson = mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerName", is("Alice Smith")))
            .andReturn().getResponse().getContentAsString();

        String appointmentId = objectMapper.readTree(responseJson).get("id").asText();
        String refNumber = objectMapper.readTree(responseJson).get("referenceNumber").asText();

        // Query by reference
        mockMvc.perform(get("/api/v1/appointments/reference/{ref}", refNumber))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(appointmentId)));

        // Search by customer
        mockMvc.perform(get("/api/v1/appointments").param("customerId", customerId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].referenceNumber", is(refNumber)));

        // Reschedule
        OffsetDateTime newStart = OffsetDateTime.of(2026, 11, 16, 14, 0, 0, 0, ZoneOffset.UTC);
        mockMvc.perform(post("/api/v1/appointments/{id}/reschedule", appointmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new com.unified.scheduler.dto.RescheduleRequestDto(newStart, "Work conflict"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startTime", notNullValue()));
    }

    @Test
    @DisplayName("shouldSearchDealershipsPaged_withPaginationMetadata")
    void shouldSearchDealershipsPaged_withPaginationMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/dealerships/search")
                .param("query", "Metro")
                .param("page", "0")
                .param("size", "5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.pageNumber", is(0)))
            .andExpect(jsonPath("$.pageSize", is(5)))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.isFirst", is(true)));
    }

    @Test
    @DisplayName("shouldSearchServiceTypesPaged_withPaginationMetadata")
    void shouldSearchServiceTypesPaged_withPaginationMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/service-types/search")
                .param("query", "Brake")
                .param("page", "0")
                .param("size", "5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("shouldSearchTechniciansPaged_withPaginationMetadata")
    void shouldSearchTechniciansPaged_withPaginationMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/technicians/search")
                .param("dealershipId", centralMetroId.toString())
                .param("page", "0")
                .param("size", "5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("shouldSearchServiceBaysPaged_withPaginationMetadata")
    void shouldSearchServiceBaysPaged_withPaginationMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/service-bays/search")
                .param("dealershipId", centralMetroId.toString())
                .param("page", "0")
                .param("size", "5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("shouldSearchAppointmentsPaged_withPaginationMetadata")
    void shouldSearchAppointmentsPaged_withPaginationMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/search")
                .param("dealershipId", centralMetroId.toString())
                .param("page", "0")
                .param("size", "5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNumber", is(0)))
            .andExpect(jsonPath("$.pageSize", is(5)));
    }

    @Test
    @DisplayName("shouldReturnCustomers_whenListingCatalog")
    void shouldReturnCustomers_whenListingCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/customers/lookup")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(4))))
            .andExpect(jsonPath("$.items[0].code", notNullValue()))
            .andExpect(jsonPath("$.items[0].label", notNullValue()))
            .andExpect(jsonPath("$.hasMore", is(false)));

        mockMvc.perform(get("/api/v1/customers")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    @DisplayName("shouldReturnHasMoreTrue_whenLookupLimitIsSmallerThanTotal")
    void shouldReturnHasMoreTrue_whenLookupLimitIsSmallerThanTotal() throws Exception {
        mockMvc.perform(get("/api/v1/customers/lookup")
                .param("page", "0")
                .param("limit", "2")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.page", is(0)))
            .andExpect(jsonPath("$.limit", is(2)))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(4)))
            .andExpect(jsonPath("$.hasMore", is(true)));
    }

    @Test
    @DisplayName("shouldReturnCustomerById_whenValidId")
    void shouldReturnCustomerById_whenValidId() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", 1L)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.customerCode", is("CUST-001")))
            .andExpect(jsonPath("$.firstName", is("John")))
            .andExpect(jsonPath("$.lastName", is("Doe")))
            .andExpect(jsonPath("$.email", is("john.doe@example.com")));
    }

    @Test
    @DisplayName("shouldReturnCustomerByCode_whenValidCode")
    void shouldReturnCustomerByCode_whenValidCode() throws Exception {
        mockMvc.perform(get("/api/v1/customers/code/{customerCode}", "CUST-001")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.customerCode", is("CUST-001")))
            .andExpect(jsonPath("$.fullName", is("John Doe")));
    }

    @Test
    @DisplayName("shouldSearchCustomersPaged_withQuery")
    void shouldSearchCustomersPaged_withQuery() throws Exception {
        mockMvc.perform(get("/api/v1/customers/search")
                .param("query", "Alice")
                .param("page", "0")
                .param("size", "5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].customerCode", is("CUST-002")))
            .andExpect(jsonPath("$.content[0].email", is("alice.smith@example.com")));
    }

    @Test
    @DisplayName("shouldReturn404_whenCustomerNotFound")
    void shouldReturn404_whenCustomerNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", 999999L)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("shouldReturn404NotFound_whenBookingForNonExistentCustomer")
    void shouldReturn404NotFound_whenBookingForNonExistentCustomer() throws Exception {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        var request = new BookingRequestDto(
            999999L,
            "1HGCR2F83HA000001",
            centralMetroId,
            brakeServiceId,
            startTime
        );

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail", containsString("Customer not found")));
    }

    @Test
    @DisplayName("shouldBookAppointment_withPreferredTechnician_andReassignToAnotherTechnician")
    void shouldBookAppointment_withPreferredTechnician_andReassignToAnotherTechnician() throws Exception {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.UTC);
        var bookingRequest = new BookingRequestDto(
            1L,
            "1HGCR2F83HA000001",
            centralMetroId,
            brakeServiceId,
            startTime,
            tech2Id
        );

        String responseContent = mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.technicianId", is(tech2Id.intValue())))
            .andExpect(jsonPath("$.technicianName", is("David Tran")))
            .andReturn().getResponse().getContentAsString();

        String appointmentId = objectMapper.readTree(responseContent).get("id").asText();

        // Reassign to tech1
        var reassignRequest = new ReassignTechnicianRequestDto(tech1Id, "Customer requested Alex");
        mockMvc.perform(post("/api/v1/appointments/{id}/reassign", appointmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reassignRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.technicianId", is(tech1Id.intValue())))
            .andExpect(jsonPath("$.technicianName", is("Alex Nguyen")));
    }

    @Test
    @DisplayName("shouldReturn400BadRequest_whenBookingOrReassigningUnqualifiedTechnician")
    void shouldReturn400BadRequest_whenBookingOrReassigningUnqualifiedTechnician() throws Exception {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 21, 10, 0, 0, 0, ZoneOffset.UTC);
        // tech4 does NOT have BRAKE_SPECIALIST skill
        var bookingRequest = new BookingRequestDto(
            1L,
            "1HGCR2F83HA000001",
            centralMetroId,
            brakeServiceId,
            startTime,
            tech4Id
        );

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.detail", containsString("not active, does not possess required skill")));

        // Now book normally, then attempt reassign to unqualified tech4
        var validBooking = new BookingRequestDto(
            1L,
            "1HGCR2F83HA000001",
            centralMetroId,
            brakeServiceId,
            startTime,
            tech1Id
        );
        String responseContent = mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBooking)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String appointmentId = objectMapper.readTree(responseContent).get("id").asText();

        var reassignRequest = new ReassignTechnicianRequestDto(tech4Id, "Try reassigning to unqualified tech");
        mockMvc.perform(post("/api/v1/appointments/{id}/reassign", appointmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reassignRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.detail", containsString("does not possess the required skill")));
    }

    @Test
    @DisplayName("shouldReturn409Conflict_whenReassigningToTechnicianWithConflict")
    void shouldReturn409Conflict_whenReassigningToTechnicianWithConflict() throws Exception {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 22, 14, 0, 0, 0, ZoneOffset.UTC);

        // App 1 with tech1
        var booking1 = new BookingRequestDto(
            1L,
            "1HGCR2F83HA000001",
            centralMetroId,
            brakeServiceId,
            startTime,
            tech1Id
        );
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking1)))
            .andExpect(status().isCreated());

        // App 2 with tech2 at same time
        var booking2 = new BookingRequestDto(
            4L,
            "4HGCR2F83HA000004",
            centralMetroId,
            brakeServiceId,
            startTime,
            tech2Id
        );
        String response2 = mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking2)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String appointment2Id = objectMapper.readTree(response2).get("id").asText();

        // Attempt reassign app 2 to tech1 (who is busy with app 1)
        var reassignRequest = new ReassignTechnicianRequestDto(tech1Id, "Should fail due to conflict");
        mockMvc.perform(post("/api/v1/appointments/{id}/reassign", appointment2Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reassignRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.detail", containsString("unavailable during slot")));
    }

    @Test
    @DisplayName("shouldSearchAppointments_byTechnicianId_whenTechnicianBelongsToDealership")
    void shouldSearchAppointments_byTechnicianId_whenTechnicianBelongsToDealership() throws Exception {
        mockMvc.perform(get("/api/v1/appointments")
                .param("dealershipId", centralMetroId.toString())
                .param("technicianId", tech1Id.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("shouldReturn400BadRequest_whenSearchingAppointments_withTechnicianNotBelongingToDealership")
    void shouldReturn400BadRequest_whenSearchingAppointments_withTechnicianNotBelongingToDealership() throws Exception {
        Long westsideId = dealershipRepository.findByCode("DLR-WEST").map(Dealership::getId).orElse(2L);

        // tech1 belongs to centralMetro, not westside!
        mockMvc.perform(get("/api/v1/appointments")
                .param("dealershipId", westsideId.toString())
                .param("technicianId", tech1Id.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.detail", containsString("does not belong to dealership with ID")));
    }

    @Test
    @DisplayName("shouldReturn405MethodNotAllowed_whenReschedulingWithEmptyIdInPath")
    void shouldReturn405MethodNotAllowed_whenReschedulingWithEmptyIdInPath() throws Exception {
        OffsetDateTime newStart = OffsetDateTime.of(2026, 11, 16, 14, 0, 0, 0, ZoneOffset.UTC);
        mockMvc.perform(post("/api/v1/appointments//reschedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RescheduleRequestDto(newStart, "Work conflict"))))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status", is(405)))
            .andExpect(jsonPath("$.type", is("urn:problem-type:method-not-allowed")))
            .andExpect(jsonPath("$.detail", containsString("please ensure the path parameter {id} is provided in the URL")));
    }

    @Test
    @DisplayName("shouldReturn409Conflict_whenSameCustomerAttemptsSimultaneousBooking")
    void shouldReturn409Conflict_whenSameCustomerAttemptsSimultaneousBooking() throws Exception {
        OffsetDateTime slotTime = OffsetDateTime.of(2026, 11, 28, 10, 0, 0, 0, ZoneOffset.UTC);

        // First booking for Customer 1
        var booking1 = new BookingRequestDto(
            1L,
            "1HGCR2F83HA111111",
            centralMetroId,
            brakeServiceId,
            slotTime,
            tech1Id
        );
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking1)))
            .andExpect(status().isCreated());

        // Second booking for SAME Customer 1 at the EXACT SAME slotTime (even with different VIN and different tech)
        var booking2 = new BookingRequestDto(
            1L,
            "1HGCR2F83HA222222",
            centralMetroId,
            brakeServiceId,
            slotTime,
            tech2Id
        );
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.title", is("Resource Conflict")))
            .andExpect(jsonPath("$.detail", containsString("already has an active appointment scheduled during slot")));
    }

    @Test
    @DisplayName("shouldReturn409Conflict_whenSameVehicleVinAttemptsSimultaneousBooking")
    void shouldReturn409Conflict_whenSameVehicleVinAttemptsSimultaneousBooking() throws Exception {
        OffsetDateTime slotTime = OffsetDateTime.of(2026, 11, 29, 10, 0, 0, 0, ZoneOffset.UTC);
        String contestedVin = "1HGCR2F83HA999888";

        // First booking for Customer 1 with contested VIN
        var booking1 = new BookingRequestDto(
            1L,
            contestedVin,
            centralMetroId,
            brakeServiceId,
            slotTime,
            tech1Id
        );
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking1)))
            .andExpect(status().isCreated());

        // Second booking for Customer 2 with the SAME contested VIN at the SAME time
        var booking2 = new BookingRequestDto(
            2L,
            contestedVin,
            centralMetroId,
            brakeServiceId,
            slotTime,
            tech2Id
        );
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.title", is("Resource Conflict")))
            .andExpect(jsonPath("$.detail", containsString("Vehicle with VIN " + contestedVin + " already has an active appointment scheduled during slot")));
    }

    @Test
    @DisplayName("shouldReturnAuditLogs_forExistingAppointment")
    void shouldReturnAuditLogs_forExistingAppointment() throws Exception {
        OffsetDateTime startTime = OffsetDateTime.of(2026, 11, 25, 9, 0, 0, 0, ZoneOffset.UTC);
        var bookingRequest = new BookingRequestDto(
            1L,
            "1HGCR2F83HA000001",
            centralMetroId,
            brakeServiceId,
            startTime
        );

        String responseContent = mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String appointmentId = objectMapper.readTree(responseContent).get("id").asText();

        // Reschedule to create a second audit log
        OffsetDateTime newTime = OffsetDateTime.of(2026, 11, 25, 10, 0, 0, 0, ZoneOffset.UTC);
        var rescheduleRequest = new RescheduleRequestDto(newTime, "Rescheduling to 10 AM", null);
        mockMvc.perform(post("/api/v1/appointments/{id}/reschedule", appointmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rescheduleRequest)))
            .andExpect(status().isOk());

        // Fetch audit logs
        mockMvc.perform(get("/api/v1/appointments/{id}/audit-logs", appointmentId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
            .andExpect(jsonPath("$[0].action", is("RESCHEDULED")))
            .andExpect(jsonPath("$[0].appointmentId", is(appointmentId)))
            .andExpect(jsonPath("$[1].action", is("BOOKED")))
            .andExpect(jsonPath("$[1].appointmentId", is(appointmentId)));
    }

    @Test
    @DisplayName("shouldReturn404NotFound_whenFetchingAuditLogsForNonExistentAppointment")
    void shouldReturn404NotFound_whenFetchingAuditLogsForNonExistentAppointment() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/appointments/{id}/audit-logs", nonExistentId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }
}
