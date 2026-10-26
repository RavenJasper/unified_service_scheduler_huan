package com.unified.scheduler.controller;

import com.unified.scheduler.domain.AppointmentStatus;
import com.unified.scheduler.dto.*;
import com.unified.scheduler.service.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Book, reschedule, cancel, and query automotive service appointments")
public class AppointmentController {

    private final SchedulingService schedulingService;

    public AppointmentController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @PostMapping
    @Operation(summary = "Schedule a new service appointment")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Appointment booked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload or business rule violation"),
        @ApiResponse(responseCode = "404", description = "Referenced dealership or service type not found"),
        @ApiResponse(responseCode = "409", description = "Requested time slot unavailable due to bay/technician conflict")
    })
    public ResponseEntity<AppointmentResponseDto> bookAppointment(@Valid @RequestBody BookingRequestDto request) {
        AppointmentResponseDto response = schedulingService.bookAppointment(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment found"),
        @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<AppointmentResponseDto> getAppointmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(schedulingService.getAppointmentById(id));
    }

    @GetMapping("/reference/{referenceNumber}")
    @Operation(summary = "Get appointment by external reference number")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment found"),
        @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<AppointmentResponseDto> getAppointmentByReference(@PathVariable String referenceNumber) {
        return ResponseEntity.ok(schedulingService.getAppointmentByReference(referenceNumber));
    }

    @GetMapping({"", "/search"})
    @Operation(summary = "Search appointments with keyword filtering (ref / VIN), status, dealership, technician and Spring Data pagination for table views")
    public ResponseEntity<PageResponseDto<AppointmentResponseDto>> searchAppointments(
        @Parameter(description = "Keyword search for reference number or vehicle VIN") @RequestParam(required = false) String query,
        @Parameter(description = "Dealership ID") @RequestParam(required = false) Long dealershipId,
        @Parameter(description = "Customer ID") @RequestParam(required = false) Long customerId,
        @Parameter(description = "Technician ID") @RequestParam(required = false) Long technicianId,
        @Parameter(description = "Appointment status filter") @RequestParam(required = false) AppointmentStatus status,
        @PageableDefault(page = 0, size = 10, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(schedulingService.searchAppointmentsPaged(query, dealershipId, customerId, technicianId, status, pageable));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an existing confirmed appointment and release resources")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment successfully cancelled"),
        @ApiResponse(responseCode = "400", description = "Appointment cannot be cancelled (e.g. already cancelled)"),
        @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<AppointmentResponseDto> cancelAppointment(
        @PathVariable UUID id,
        @Valid @RequestBody CancellationRequestDto request
    ) {
        return ResponseEntity.ok(schedulingService.cancelAppointment(id, request));
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule an existing confirmed appointment to a new time slot")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment successfully rescheduled"),
        @ApiResponse(responseCode = "400", description = "Invalid new time slot or business rule violation"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "409", description = "Target new time slot is unavailable")
    })
    public ResponseEntity<AppointmentResponseDto> rescheduleAppointment(
        @PathVariable UUID id,
        @Valid @RequestBody RescheduleRequestDto request
    ) {
        return ResponseEntity.ok(schedulingService.rescheduleAppointment(id, request));
    }

    @PostMapping("/{id}/reassign")
    @Operation(summary = "Reassign an appointment to another qualified technician")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment successfully reassigned to new technician"),
        @ApiResponse(responseCode = "400", description = "Invalid technician or appointment state"),
        @ApiResponse(responseCode = "404", description = "Appointment or technician not found"),
        @ApiResponse(responseCode = "409", description = "Technician is unavailable / busy in this time slot")
    })
    public ResponseEntity<AppointmentResponseDto> reassignTechnician(
        @PathVariable UUID id,
        @Valid @RequestBody ReassignTechnicianRequestDto request
    ) {
        return ResponseEntity.ok(schedulingService.reassignTechnician(id, request));
    }

    @GetMapping({"/{id}/audit-logs", "/{id}/audit"})
    @Operation(summary = "Get audit trail logs for a specific appointment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit trail logs returned successfully"),
        @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    public ResponseEntity<List<AppointmentAuditLogDto>> getAppointmentAuditLogs(@PathVariable UUID id) {
        return ResponseEntity.ok(schedulingService.getAppointmentAuditLogs(id));
    }
}
