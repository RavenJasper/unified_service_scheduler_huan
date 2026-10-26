# Implementation Plan - The Unified Service Scheduler (USS)

## 1. Project Goal & Executive Overview
The objective is to engineer a production-ready, enterprise-grade Backend Service for **The Unified Service Scheduler (USS)** within the automotive **Ownership** domain. The service replaces fragmented, manual booking workflows with an automated, resource-constrained scheduling engine that simultaneously allocates physical **Service Bays** and **Qualified Technicians** for requested vehicle services at dealership locations.

The deliverables fulfill both parts of the customer assignment:
1. **Part 1: System Design Document** ([`docs/system-design.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/system-design.md)): Comprehensive architectural blueprint, C4 & sequence diagrams, component responsibilities, data flow, technology stack trade-off justifications, observability strategy, and a dedicated section on GenAI design collaboration.
2. **Part 2: Service Implementation (Backend)**: Production-grade Spring Boot 3 + Java 21 service backed by persistent PostgreSQL 17 storage, Flyway migrations, pessimistic row-level concurrency locking (zero double-booking guarantee), OpenAPI 3.0 / Swagger UI, automated test suite (Unit, Slice, MockMvc, Concurrency Stress Test), and a ready-to-run Bruno API Collection.

---

## 2. Key Architecture & Concurrency Strategy

> [!IMPORTANT]
> **Zero Double-Booking Concurrency Strategy**: Under high contention (e.g. 50 parallel requests targeting the exact same final slot), the system guarantees that strictly one booking succeeds and all other requests receive an immediate `409 Conflict` (RFC 7807 Problem Details). This is achieved through native PostgreSQL row-level pessimistic locking (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`) within an isolated ACID transaction boundary, paired with composite B-tree indexes on temporal ranges `(bay_id, start_time, end_time)` and `(technician_id, start_time, end_time)`.

> [!NOTE]
> **Self-Contained Automated Testing**: The test profile (`application-test.yml`) leverages an in-memory database in PostgreSQL compatibility mode with Flyway enabled. This allows the full test suite—including multi-threaded concurrency stress tests—to execute deterministically via `mvn clean test` with zero external dependencies.

---

## 3. Phased Execution Plan

### Phase 1: Requirements, Rules, Skills & Assumptions (Completed)
- [x] Author Software Requirements Specification: [`docs/requirements.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/requirements.md).
- [x] Author Architectural Assumptions & Technical Trade-offs: [`docs/assumption-and-tradeoff.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/assumption-and-tradeoff.md).
- [x] Establish modular, specialized engineering rules:
  - Database Design & Script Rules: [`docs/rules/database-design-and-script-rules.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/rules/database-design-and-script-rules.md).
  - Java 21 Coding Standards: [`docs/rules/java-coding-standards.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/rules/java-coding-standards.md).
  - Spring Boot Architecture Rules: [`docs/rules/spring-boot-architecture-rules.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/rules/spring-boot-architecture-rules.md).
  - Unit & Integration Testing Rules: [`docs/rules/unit-and-integration-testing-rules.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/rules/unit-and-integration-testing-rules.md).
  - AI Engineering & Verification Rules: [`docs/rules/ai-engineering-and-verification-rules.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/rules/ai-engineering-and-verification-rules.md).
- [x] Author custom workspace review skills in `.agents/skills/` and `docs/skills/`:
  - `uss-code-reviewer`: Java 21 & Spring Boot 3 code audit skill.
  - `uss-db-script-reviewer`: PostgreSQL schema & Flyway migration review skill.
  - `uss-test-and-concurrency-verifier`: Test suite execution & concurrency proof skill.
- [x] Author Bruno API verification collection in `bruno/`.
- [x] Author project documentation in [`README.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/README.md).

---

### Phase 2: System Design Document (Part 1 Deliverable)
- Author [`docs/system-design.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/system-design.md):
  - **System Architecture Diagram**: C4 Context, Container, and Component diagrams using Mermaid.
  - **Component Roles & Responsibilities**: REST Controllers, Scheduling Engine, Resource Allocator, Persistence & Audit.
  - **Data Flow Explanation**: Sequence diagrams for real-time slot discovery and atomic concurrent appointment reservation.
  - **Technology Justification**: In-depth trade-off rationale for Java 21, Spring Boot 3, PostgreSQL 17, Flyway, and Bruno.
  - **Observability Strategy**: Structured JSON logging with MDC `traceId`, Micrometer Prometheus metrics, and Actuator health probes.
  - **GenAI Design Assistance**: Dedicated section documenting how GenAI was directed during problem decomposition, constraint formulation, and testing architecture design.

---

### Phase 3: Database Schema Design & Flyway Migrations
*Core database foundation ensuring persistence and ACID-compliant concurrency control.*

1. **Schema Design & Data Modeling**:
   - Primary Keys: `id UUID DEFAULT gen_random_uuid()`.
   - Timestamps: All temporal columns defined as `TIMESTAMP WITH TIME ZONE` (`TIMESTAMPTZ`).
   - String bounds: VIN `VARCHAR(17)`, reference number `VARCHAR(32)`, titles `VARCHAR(100)`.
   - Status Enums: `VARCHAR(30)` with check constraint (`CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `RESCHEDULED`).
   - Mandatory audit columns on core tables: `created_at`, `updated_at`, `version`.
   - Core tables: `dealerships`, `service_types`, `service_bays`, `technicians`, `technician_skills`, `appointments`, `appointment_audit_logs`.
2. **Flyway Migration Scripts**:
   - `src/main/resources/db/migration/V1__init_schema.sql`: Full DDL, primary/foreign keys, check constraints, and composite B-tree indexes on `(dealership_id, start_time, end_time)`, `(service_bay_id, start_time, end_time)`, and `(technician_id, start_time, end_time)`.
   - `src/main/resources/db/migration/V2__seed_data.sql`: Realistic seed dataset (dealership branch, 5 bays, 6 technicians with skill sets, service catalogue with standard durations).
3. **Pessimistic Locking & Zero Double-Booking Semantics**:
   - Active appointment filter (`CONFIRMED`, `IN_PROGRESS`) in reservation queries. Cancelled appointments immediately release resources.
4. **HikariCP Connection Pool Tuning**:
   - `maximum-pool-size: 20`, `minimum-idle: 5`, `connection-timeout: 30000ms`, `idle-timeout: 600000ms`.

---

### Phase 4: Project Scaffolding & Spring Boot 3 Configuration
- Create `pom.xml` with dependencies:
  - `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`.
  - `spring-boot-starter-actuator`, `micrometer-registry-prometheus`.
  - `org.postgresql:postgresql`, `flyway-core`, `flyway-database-postgresql`.
  - `com.h2database:h2` (for test profile).
  - `springdoc-openapi-starter-webmvc-ui` (Swagger UI).
  - `spring-boot-starter-test`, `assertj-core`.
- Configure `src/main/resources/application.yml` (PostgreSQL connection, Flyway auto-migration, HikariCP, JPA settings).
- Configure `src/main/resources/application-test.yml` (In-memory H2 PostgreSQL mode for fast independent test runs).

---

### Phase 5: Domain Model & Persistence Layer
- Entities in `com.unified.scheduler.domain`:
  - `Dealership`, `ServiceType`, `ServiceBay`, `Technician`, `TechnicianSkill`, `Appointment`, `AppointmentAuditLog`.
  - Enums: `AppointmentStatus` (Dynamic string-based skill codes for extensibility).
- Repositories in `com.unified.scheduler.repository`:
  - `AppointmentRepository`: Custom queries with `@Lock(LockModeType.PESSIMISTIC_WRITE)` for overlap checks.
  - `TechnicianRepository`: Query technicians matching skill qualifications and active dealership employment.
  - `ServiceBayRepository`: Query service bays belonging to the specified dealership branch.

---

### Phase 6: Core Scheduling Engine & Concurrency Control
- Services in `com.unified.scheduler.service`:
  - `SlotCalculator`: Pure algorithmic utility calculating valid 30-minute interval slots across operating hours.
  - `SchedulingService`:
    - `getAvailableSlots(dealershipId, serviceTypeId, date)`: Computes real-time availability by verifying simultaneous existence of at least 1 vacant bay and 1 qualified technician per slot.
    - `bookAppointment(BookingRequest)`: Validates inputs -> acquires atomic pessimistic lock on bay & technician -> persists confirmed appointment -> records audit log. Throws `ResourceUnavailableException` (HTTP 409) if resources are unavailable.
    - `cancelAppointment(appointmentId, reason)`: Releases bay and technician allocations, transitions state to `CANCELLED`.
    - `rescheduleAppointment(appointmentId, request)`: Atomically validates new slot, reassigns resources, and updates appointment record.
- Domain Exceptions:
  - `ResourceNotFoundException` (404), `ResourceUnavailableException` (409 Conflict), `InvalidBookingException` (400/422).

---

### Phase 7: REST API Layer, OpenAPI & Global Exception Handler
- DTO Records in `com.unified.scheduler.dto`:
  - `BookingRequestDto`, `AppointmentResponseDto`, `AvailableSlotDto`, `AvailableBayDto`, `RescheduleRequestDto`, `CancellationRequestDto`, `ReassignTechnicianRequestDto`, `AppointmentAuditLogDto`, `CustomerDto`, `ServiceBayDto`, `TechnicianDto`, `PageResponseDto`.
- REST Controllers in `com.unified.scheduler.controller`:
  - `AppointmentController`: Endpoints for booking (with preferred technician support), retrieval by ID/reference, paginated search with technician filter and cross-dealership validation, rescheduling, reassignment (`POST /{id}/reassign`), cancellation with `@JsonAlias` reason support, and audit log inspection (`GET /{id}/audit-logs`).
  - `ScheduleController`: Real-time slot discovery endpoint (`/api/v1/schedules/available-slots`) returning both `availableBays` and `availableTechnicians`.
  - `DealershipController`, `ServiceTypeController`, `TechnicianController`, `ServiceBayController`, `CustomerController`: Complete metadata and master data browsing and paginated search endpoints.
- `GlobalExceptionHandler`: Centralized `@RestControllerAdvice` mapping exceptions to standard **RFC 7807 (`ProblemDetail`)**, including specialized handlers for `HttpRequestMethodNotSupportedException` (405) and `NoResourceFoundException` (404).
- OpenAPI 3.0 / Swagger UI annotations and interactive console at `/swagger-ui.html`.

---

### Phase 8: Observability, Metrics & Client Test Harness
- Structured JSON logging configuration in `logback-spring.xml` with MDC `traceId` correlation filter.
- Custom Micrometer Prometheus metrics: `appointments.booked.total`, `appointments.rejected.conflict`, `scheduling.duration`.
- Actuator endpoints exposed: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`.
- Bruno API Collection verification in `bruno/` (all 5 directories updated with complete test suites) and cURL test harness in `docs/curl-test-harness.sh`.

---

### Phase 9: Automated Testing Suite & AI Verification
- **Unit Tests**:
  - `SchedulingServiceTest`: Mockito-based isolated unit tests for all business flows, edge cases, technician reassignment, customer & vehicle VIN conflict guards, and audit log inspection (24 tests).
  - `SlotCalculatorTest`: Boundary, grid alignment, operating hours, and interval overlap calculation tests (6 tests).
  - `CustomerServiceTest`, `DealershipServiceTest`, `ServiceTypeServiceTest`, `TechnicianServiceTest`, `ServiceBayServiceTest`: Full coverage of master data queries and paginated searches (22 tests).
  - `DomainModelTest`: Entity domain validation (6 tests).
- **Slice / Integration Tests**:
  - `AppointmentControllerIntegrationTest`: Full HTTP lifecycle testing via `MockMvc` covering all positive and negative branches, boundary validations, conflict checks, and error responses (41 tests).
- **Concurrency Stress Test**:
  - `AppointmentConcurrencyTest`: Multi-threaded test using `ExecutorService` and `CountDownLatch` with 20 parallel threads competing for 1 slot. Asserts exactly 1 booking succeeds and all 19 others receive HTTP 409 Conflict with zero database double-booking (1 test).
- **AI Collaboration Log**:
  - [`docs/ai-collaboration-log.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/ai-collaboration-log.md): Formal log documenting prompt history, architectural trade-offs, and empirical verification records across 26 entries.

---

## 4. Verification & Acceptance Criteria Checklist

### Automated Verification
```bash
mvn clean test
```
- [x] 100% of unit, slice, and integration tests pass with 0 failures and 0 errors (100/100 passing).
- [x] Concurrency stress test proves zero double-booking under race conditions (20 concurrent threads, 1 success, 19 conflicts).
- [x] Code coverage exceeds target: achieved 91.70% line coverage and 86.95% instruction coverage (exceeding >= 85% requirement).

### Manual & Client Verification
1. Start the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
2. Verify endpoints via Bruno Collection (`bruno/`) across all folders (`01-catalogs` to `05-observability`).
3. Verify interactive Swagger UI documentation at `http://localhost:8080/swagger-ui.html`.
4. Verify Actuator health probe at `http://localhost:8080/actuator/health`.
