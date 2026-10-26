# System Design Document: The Unified Service Scheduler (USS)

**Document Version**: 1.0  
**Domain**: Ownership (Automotive Service & Dealership Network)  
**Author**: Systems Architecture & AI Engineering Team  
**Evaluation Scope**: Keyloop Technical Assessment - Scenario A  

---

## 1. Executive Summary & Problem Framing

### 1.1. Context & Business Problem
Traditional automotive dealerships rely on fragmented, manual, or phone-based service booking processes. This operational friction results in:
- Inefficient physical service stall (bay) utilization and idle downtime.
- Incompatible technician assignment (e.g., assigning a technician without high-voltage electrical certification to perform an EV battery diagnostic).
- Double-booking conflicts under concurrent customer booking rushes.
- Lack of immutable audit trails for appointments and vehicle ownership histories.

### 1.2. System Mission
**The Unified Service Scheduler (USS)** is a resilient, enterprise-grade backend service designed to automate resource-constrained scheduling. The system enforces strict multi-resource availability rules: an appointment is only confirmed when **both** an eligible physical **Service Bay** AND an active **Qualified Technician** are simultaneously vacant and locked for the entire duration of the requested service.

---

## 2. System Architecture & Diagrams

The system architecture is modeled using the **C4 Architectural Model** (Context, Container, Component).

### 2.1. Level 1: System Context Diagram
The System Context diagram illustrates how USS interfaces directly with real-world users, reflecting its self-contained architecture:

```mermaid
flowchart TD
    classDef actor fill:#e1f5fe,stroke:#0288d1,stroke-width:2px,color:#01579b;
    classDef core fill:#e8f5e9,stroke:#388e3c,stroke-width:2px,color:#1b5e20;

    subgraph ACTORS["SYSTEM ACTORS"]
        direction LR
        CUST["<b>Vehicle Owner / Customer</b><br/>Web & Mobile Portal<br/><i>(Slot Discovery, Booking, Rescheduling)</i>"]:::actor
        ADV["<b>Service Advisor / Staff</b><br/>Web Portal / Swagger UI / Bruno<br/><i>(Catalog Admin, Dispatching, Monitoring)</i>"]:::actor
    end

    USS["<b>The Unified Service Scheduler (USS)</b><br/>Self-Contained Automated Workshop Scheduling Service<br/><i>(Customer & Bay Management, Pessimistic Locking, Zero Double-Booking)</i>"]:::core

    CUST -->|1. HTTPS / REST: Slots & Booking| USS
    ADV -->|2. HTTPS / REST: Workshop Dispatching| USS
```

---

### 2.2. Level 2: Container Diagram (Backend Infrastructure)
The Container diagram illustrates the actual technical boundaries between API clients, the Spring Boot application container, and the PostgreSQL database:

```mermaid
flowchart LR
    classDef client fill:#e1f5fe,stroke:#0288d1,stroke-width:2px,color:#01579b;
    classDef app fill:#e8f5e9,stroke:#388e3c,stroke-width:2px,color:#1b5e20;
    classDef db fill:#eceff1,stroke:#455a64,stroke-width:2px,color:#263238;

    CLIENT["<b>API Client / User Interface</b><br/>Web App, Mobile App<br/><i>Swagger UI & Bruno API Suite</i>"]:::client

    subgraph USS_BACKEND["USS BACKEND INFRASTRUCTURE"]
        direction LR
        APP["<b>USS Spring Boot Application</b><br/>Java 21 / Spring Boot 3.3<br/><i>REST Controllers, Scheduling Engine, Actuator</i>"]:::app
        DB[("<b>PostgreSQL Database</b><br/>PostgreSQL 17<br/><i>ACID Transactions, Row Locks, Flyway DDL</i>")]:::db
    end

    CLIENT -->|1. HTTP / REST API - Port 8080| APP
    APP -->|2. JDBC / HikariCP Pool - Port 5432| DB
```

---

### 2.3. Level 3: Component Diagram (Spring Boot Internal Architecture)
The Component diagram depicts the internal modularity and strict one-way layered architecture inside the Spring Boot application container:

```mermaid
flowchart TD
    classDef web fill:#e1f5fe,stroke:#0288d1,stroke-width:2px,color:#01579b;
    classDef service fill:#e8f5e9,stroke:#388e3c,stroke-width:2px,color:#1b5e20;
    classDef repo fill:#fff3e0,stroke:#f57c00,stroke-width:2px,color:#e65100;
    classDef cross fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#4a148c;
    classDef db fill:#eceff1,stroke:#455a64,stroke-width:2px,color:#263238;

    subgraph PresentationLayer["1. PRESENTATION LAYER (Web & API)"]
        direction LR
        CTRL["<b>REST Controllers</b><br/>Appointment, Schedule, Catalogs<br/><i>(Jakarta Validation, DTO Mapping)</i>"]:::web
        EXH["<b>Global Exception Handler</b><br/>@RestControllerAdvice<br/><i>(RFC 7807 ProblemDetail)</i>"]:::web
    end

    subgraph ServiceLayer["2. DOMAIN & SCHEDULING LAYER (Core Service)"]
        direction LR
        SCHED["<b>SchedulingServiceImpl</b><br/>Transaction & Resource Orchestrator<br/><i>(@Transactional, Pessimistic Locking)</i>"]:::service
        CALC["<b>SlotCalculator</b><br/>30-Min Discrete Grid Engine<br/><i>(com.unified.scheduler.util)</i>"]:::service
    end

    subgraph PersistenceLayer["3. PERSISTENCE LAYER (Data Access)"]
        direction LR
        REPO["<b>Spring Data JPA Repositories</b><br/>Appointment, Bay, Technician<br/><i>(PESSIMISTIC_WRITE & Specifications)</i>"]:::repo
    end

    subgraph CrossCutting["4. CROSS-CUTTING CONCERNS (Logging & Audit)"]
        direction TB
        AUDIT["<b>Audit Logging Engine</b><br/>Immutable State Change Tracking<br/><i>(appointment_audit_logs)</i>"]:::cross
        LOG["<b>Logging & Health Probes</b><br/>MDC traceId & Actuator<br/><i>(MdcLoggingFilter, /actuator/health)</i>"]:::cross
    end

    DB[("<b>PostgreSQL 17</b><br/>ACID Transactions, Row Locks")]:::db

    %% Primary execution flow (Top-Down)
    CTRL -->|1. Delegates DTO & processing| SCHED
    CTRL -.->|Catches & standardizes errors| EXH
    CTRL -.->|Attaches traceId to context| LOG
    SCHED -->|2. Computes available windows| CALC
    SCHED -->|3. Queries & locks resources| REPO
    REPO -->|4. SQL FOR UPDATE & Commit| DB

    %% Cross-cutting telemetry (Dashed)
    SCHED -.->|5. Persists audit records| AUDIT
```

---

## 3. Component Roles & Responsibilities

| Component Name | Primary Architectural Responsibility | Key Classes / Interfaces |
| :--- | :--- | :--- |
| **REST Controller Layer** | Handles incoming HTTP requests, enforces Jakarta Bean Validation (`@Valid`), delegates to the service layer, and maps internal records to API response DTOs. Never interacts directly with database repositories. | `AppointmentController`, `ScheduleController`, `DealershipController`, `ServiceTypeController` |
| **Global Exception Handler** | Intercepts all unchecked exceptions, domain failures, and validation errors. Formats every error as an RFC 7807 `ProblemDetail` document with machine-readable metadata. | `GlobalExceptionHandler`, `BusinessException`, `ResourceUnavailableException` |
| **Scheduling Engine** | Core orchestrator of business logic. Coordinates availability calculation across multi-resource constraints (bays + technicians), triggers atomic row locks, and commits appointments. | `SchedulingService`, `SchedulingServiceImpl` |
| **Slot Calculator** | Deterministic mathematical utility that divides dealership operating hours into standard 30-minute grid increments and evaluates resource overlap matrices. | `SlotCalculator` |
| **Persistence & Repository Layer** | Interfaces with PostgreSQL 17 via Spring Data JPA. Contains custom JPQL and native queries using `PESSIMISTIC_WRITE` locks to serialize competing transactions. | `AppointmentRepository`, `TechnicianRepository`, `ServiceBayRepository` |
| **Audit & Event Engine** | Records immutable historical state transitions (`CONFIRMED`, `CANCELLED`, `RESCHEDULED`) into `appointment_audit_logs` for compliance and operational transparency. | `AppointmentAuditLog`, `AppointmentEventListener` |
| **Logging & Health Probes** | Structured JSON logging with Slf4j/Logback MDC `traceId` correlation and Spring Boot Actuator `/actuator/health` probes. | `MdcLoggingFilter`, Spring Boot Actuator |

---

## 4. End-to-End Data Flow & Sequence Diagrams

### 4.1. Sequence 1: Real-Time Multi-Resource Slot Discovery
*Scenario*: A customer searches for available appointment slots at Dealership $D$ for Service Type $S$ over a Date Range $[T_1, T_2]$.

```mermaid
sequenceDiagram
    autonumber
    actor Client as API Client / User
    participant Controller as ScheduleController
    participant Service as SchedulingService
    participant Calculator as SlotCalculator
    participant DB as DB Repositories

    Client->>Controller: GET /available-slots<br/>?dealershipId=1&serviceTypeId=2
    Controller->>Service: getAvailableSlots<br/>(dealership, serviceType, dateRange)

    Service->>DB: Fetch operating hours<br/>(08:00 - 18:00)
    Service->>DB: Fetch duration (60m)<br/>& required skill (BRAKE)
    Service->>Calculator: generateCandidateSlots<br/>(08:00 - 18:00, 30m grid)
    Calculator-->>Service: Candidate start timestamps
    Service->>DB: findActiveAppointmentsInWindow<br/>(windowStart, windowEnd)

    loop For each candidate slot
        Service->>Calculator: overlaps(slot, appointment)
        Service->>Service: evaluateSlotAvailability<br/>(freeBays, freeTechs)
    end

    Service-->>Controller: AvailabilityScheduleResponseDto
    Controller-->>Client: 200 OK (Daily Available Slots)
```

---

### 4.2. Sequence 2: Atomic Multi-Resource Booking & Concurrency Protection
*Scenario*: Multiple concurrent requests attempt to reserve the exact same remaining bay/technician slot at 09:00:00.

```mermaid
sequenceDiagram
    autonumber
    actor ClientA as Client A (Thread 1)
    actor ClientB as Client B (Thread 2)
    participant Controller as AppointmentController
    participant Service as SchedulingService
    participant DB as PostgreSQL DB

    Note over ClientA,ClientB: Race condition: Competing for 09:00:00 (1 bay, 1 tech)

    ClientA->>Controller: POST /appointments<br/>(Slot 09:00:00)
    ClientB->>Controller: POST /appointments<br/>(Slot 09:00:00)

    Controller->>Service: bookAppointment(A)
    Controller->>Service: bookAppointment(B)

    rect rgb(240, 248, 255)
        Note over Service,DB: Tx A begins (@Transactional)
        Service->>DB: SELECT * FROM bays/techs<br/>ORDER BY id FOR UPDATE
        Note over DB: Tx A acquires Pessimistic Row Lock
    end

    rect rgb(255, 240, 245)
        Note over Service,DB: Tx B begins (@Transactional)
        Service->>DB: SELECT * FROM bays/techs<br/>ORDER BY id FOR UPDATE
        Note over DB: Tx B BLOCKS (Waiting for Tx A lock release)
    end

    Service->>DB: INSERT INTO appointments<br/>(CONFIRMED, bay, tech)
    Service->>DB: INSERT INTO audit_logs<br/>(BOOKED)
    Note over Service,DB: Tx A COMMITS (Locks released)
    Service-->>Controller: AppointmentResponseDto (201)
    Controller-->>ClientA: 201 Created (Booking Confirmed)

    rect rgb(255, 240, 245)
        Note over DB: Tx B unblocks, acquires row lock
        Service->>DB: Check availability in [09:00 - 10:00)
        Note over Service: Detection: 0 bays / 0 techs remaining!
        Service->>Service: Throw ResourceUnavailableException
        Note over Service,DB: Tx B ROLLS BACK
    end

    Service-->>Controller: Intercepted by GlobalExceptionHandler
    Controller-->>ClientB: 409 Conflict (Slot Unavailable)
```

---

### 4.3. Sequence 3: Appointment Cancellation & Resource Liberation
*Scenario*: An appointment is cancelled by the customer or advisor, immediately restoring resource capacity.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Advisor
    participant Controller as AppointmentController
    participant Service as SchedulingService
    participant DB as PostgreSQL DB

    Client->>Controller: POST /{id}/cancel<br/>(reason)
    Controller->>Service: cancelAppointment<br/>(id, reason)
    
    Service->>DB: SELECT * FROM appointments<br/>WHERE id = :id FOR UPDATE
    alt Appointment not found
        Service-->>Controller: 404 Not Found
    else Appointment is already CANCELLED
        Service-->>Controller: 400 Bad Request<br/>(Already cancelled)
    else Appointment is CONFIRMED
        Service->>DB: UPDATE appointments<br/>SET status = 'CANCELLED'
        Service->>DB: INSERT INTO audit_logs<br/>(CANCELLED, reason)
        Service-->>Controller: AppointmentResponseDto
        Controller-->>Client: 200 OK (Status: CANCELLED)
        Note over DB: Bay and Tech are immediately freed for new queries!
    end
```

---

### 4.4. Sequence 4: Technician Reassignment & Conflict Guard
*Scenario*: A service advisor manually reassigns a technician to a confirmed appointment, enforcing dealership matching, skill eligibility, and slot conflict checks.

```mermaid
sequenceDiagram
    autonumber
    actor Advisor as Service Advisor
    participant Controller as AppointmentController
    participant Service as SchedulingService
    participant DB as PostgreSQL DB

    Advisor->>Controller: POST /{id}/reassign<br/>(targetTechId)
    Controller->>Service: reassignTechnician<br/>(id, targetTechId)

    Service->>DB: SELECT * FROM appointments<br/>WHERE id = :id FOR UPDATE
    Service->>DB: SELECT * FROM techs<br/>WHERE id = :techId FOR UPDATE

    alt Technician belongs to different dealership
        Service-->>Controller: 400 Bad Request<br/>(Different branch)
    else Technician lacks required SkillCode
        Service-->>Controller: 400 Bad Request<br/>(Unqualified)
    else Technician already booked in slot window
        Service-->>Controller: 409 Conflict<br/>(Conflicting booking)
    else Valid & Available
        Service->>DB: UPDATE appointments<br/>SET technician_id = :newId
        Service->>DB: INSERT INTO audit_logs<br/>(REASSIGNED)
        Service-->>Controller: AppointmentResponseDto
        Controller-->>Advisor: 200 OK (Technician updated)
    end
```

---

## 5. Technology Choices & Detailed Justifications

```mermaid
graph TD
    subgraph Architecture Justification Matrix
        A["Java 21 LTS<br/>(Records, JSR-310, Loom)"] --> T1["Core Language"]
        B["Spring Boot 3.3+<br/>(Transactions, Data JPA)"] --> T2["Backend Framework"]
        C["PostgreSQL 17<br/>(Row Locks, TIMESTAMPTZ)"] --> T3["Database Engine"]
        D["Flyway Migrations<br/>(Versioned Immutable DDL)"] --> T4["Schema Governance"]
        E["OpenAPI 3 & Bruno<br/>(Interactive UI, Git-friendly)"] --> T5["Contracts & Tests"]
    end
```

### 5.1. Language & Runtime: Java 21 LTS - Practical Features Utilized in USS

The selection of **Java 21 LTS** as the core backend runtime is directly justified by the modern language features actively utilized across the USS codebase:

1. **Java Records (`record` - JEP 395) - 100% Request/Response DTOs & Internal Value Objects**:
   - Employed across all 21 DTO and value object files in the project (e.g. `BookAppointmentRequest`, `AppointmentResponseDto`, `AvailableSlotDto`, `DateRange`, `AllocatedResources`).
   - **Absolute Immutability**: All fields are implicitly `final`, eliminating data leakage or accidental state mutations across architectural layers and concurrent threads.
   - **Immutable DTO Standardization**: Native compiler-generated canonical constructors, getters, `equals()`, `hashCode()`, and `toString()`, ensuring API contracts remain strictly immutable without annotations; while Project Lombok is disciplined strictly to `@Getter` and `@Setter` on JPA entities.

2. **Sequenced Collections (`getFirst()` & `getLast()` - JEP 431) - Window Boundary Derivation**:
   - Applied directly in `SchedulingServiceImpl.java` to extract candidate appointment boundaries:
     ```java
     OffsetDateTime windowStart = slots.getFirst();
     OffsetDateTime windowEnd = slots.getLast().plusMinutes(durationMinutes);
     ```
   - Replaces error-prone index arithmetic (`slots.get(0)`, `slots.get(slots.size() - 1)`) with expressive, readable, and bounds-safe collection semantics.

3. **JSR-310 Modern Date/Time API (`java.time`) - Multi-Timezone Temporal Scheduling**:
   - Forms the mathematical foundation across 18 source files (particularly `SlotCalculator` and `SchedulingServiceImpl`):
     - `OffsetDateTime`: Normalized ISO-8601 UTC storage and interval arithmetic matching PostgreSQL 17 `TIMESTAMPTZ`.
     - `LocalTime` & `LocalDate`: Dealership operating window management (`openingTime`, `closingTime`) for discrete 30-minute grid generation.
     - `ZoneId`: Projects UTC timestamps to the dealership's specific regional timezone (`ZoneId.of(dealership.getTimeZone())`), ensuring Daylight Saving Time (DST) resilience.

4. **Stream API `toList()` (Java 16+) - Concise & Unmodifiable Collections**:
   - Used across service implementations (`CustomerServiceImpl`, `DealershipServiceImpl`, `ServiceBayServiceImpl`, `ServiceTypeServiceImpl`, `TechnicianServiceImpl`):
     ```java
     return repository.findAll().stream()
         .map(this::toDto)
         .toList();
     ```
   - Replaces verbose `.collect(Collectors.toList())`, directly returning an unmodifiable list to prevent downstream mutation bugs.

5. **Enhanced `Optional` Functional Idioms - Expressive Domain Error Handling**:
   - Leverages `Optional.orElseThrow(...)`, `Optional.map(...)`, and `Optional.filter(...)` throughout `SchedulingServiceImpl` to fail-fast on missing resources, cleanly throwing `BusinessException` and `ResourceUnavailableException` without nested null checks.

### 5.2. Framework: Spring Boot 3.3+
- **Chosen**: Spring Boot 3.3.x on Spring Framework 6.x.
- **Justification**:
  - **Declarative Transaction Management (`@Transactional`)**: Seamless boundaries controlling database lock acquisition, rollback semantics, and commit lifecycles.
  - **Spring Data JPA**: Expressive repository interfaces combined with native lock annotation `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
  - **Spring Boot Actuator & Micrometer**: Out-of-the-box production readiness with zero custom plumbing for metrics, health checks, and tracing.

### 5.3. Database Engine & Pragmatic Hybrid Key Architecture: PostgreSQL 17
- **Chosen**: PostgreSQL 17 with a **Hybrid Primary Key Strategy**.
- **Architectural Justification**:
  - **Master & Reference Catalog Data (`BIGINT IDENTITY`)**: Catalog tables (`dealerships`, `service_types`, `service_bays`, `technicians`, `technician_skills`) use `BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY` combined with natural business unique keys (e.g. `uk_dealerships_code`).
    - *Storage Efficiency*: 8 bytes per FK instead of 16 bytes, cutting composite B-Tree index sizes on high-frequency scheduling lookup indexes (`(service_bay_id, start_time, end_time)` and `(technician_id, start_time, end_time)`) by up to 50%.
    - *Seed & Migration Maintainability*: Eliminates awkward synthetic UUID constants (`11111111-1111...`) in seed scripts; foreign keys resolve naturally via standard ANSI SQL subqueries `(SELECT id FROM dealerships WHERE code = 'DLR-METRO')`.
  - **Transactional Domain Data (`UUID`)**: Highly sensitive business entities (`appointments`, `appointment_audit_logs`) use `UUID PRIMARY KEY` (`DEFAULT gen_random_uuid()`).
    - *Anti-Enumeration & Security*: Prevents IDOR (Insecure Direct Object Reference) vulnerabilities and prevents external scraping or competitor enumeration of dealership booking volumes.
    - *Distribution Readiness*: Enables decentralized ID generation and horizontal scalability across future microservices.
  - **ACID Strictness & Row-Level Locking**: High-performance support for `SELECT ... FOR UPDATE`, guaranteeing serializable reservation behavior without deadlocking entire tables.
  - **Timezone Precision**: Standard `TIMESTAMP WITH TIME ZONE` (`TIMESTAMPTZ`) for unambiguous multi-location dealership scheduling across regional timezones.
- **Alternatives Evaluated**:
  - *Pure UUID Everywhere*: Rejected due to high B-Tree page fragmentation, 2x foreign key storage overhead in appointment composite indexes, and awkward seed migration ergonomics.
  - *Pure Auto-Increment BIGINT Everywhere*: Rejected for `appointments` due to severe security vulnerabilities (predictable IDs expose customer volume and invite enumeration attacks).
  - *MongoDB*: Document stores lack multi-row ACID locks across separate collections (bays, technicians, appointments), creating critical race conditions and risk of double-booking.

### 5.4. Schema Versioning: Flyway
- **Chosen**: Flyway Database Migrations (`flyway-core`, `flyway-database-postgresql`).
- **Justification**:
  - Enforces version-controlled, immutable database evolution (`V1__init_schema.sql`, `V2__seed_data.sql`).
  - Guarantees complete environmental parity between local development, testing, and production.
  - Avoids dangerous Hibernate `ddl-auto: update` behaviors.

### 5.5. Client Verification & Contract: OpenAPI 3.0 & Bruno
- **Chosen**: Springdoc OpenAPI (Swagger UI) + Bruno Collection (`bruno/`).
- **Justification**:
  - **OpenAPI 3.0**: Provides an interactive browser console at `/swagger-ui.html` for ad-hoc manual verification.
  - **Bruno**: Unlike Postman, Bruno stores requests as plain text `.bru` files directly inside the Git repository (`bruno/`), enabling version control, team collaboration, and automated CLI execution without cloud account lock-in.

---

## 6. Observability & Operational Readiness

```mermaid
flowchart LR
    subgraph Observability Pipeline
        A["HTTP Request"] --> B["MDC Logging Filter"]
        B --> C["Structured JSON Log<br/>(traceId, dealershipId)"]
        C --> D["Console stdout"]

        E["Operator / Developer"] --> F["/actuator/health<br/>(Liveness & Readiness)"]
        F --> G["PostgreSQL 17 Connection Check"]
    end
```

### 6.1. Structured Logging with MDC Correlation
- **Mechanism**: Every incoming request passes through an `MdcLoggingFilter` that generates or propagates a unique `traceId` (conforming to W3C Trace Context).
- **Log Format**: JSON formatted logs via Logback (`logback-spring.xml`):
  ```json
  {
    "timestamp": "2026-10-26T09:00:00.125Z",
    "level": "INFO",
    "traceId": "c43020d2-622c-4f2f-8adc-370233fadcd1",
    "thread": "http-nio-8080-exec-3",
    "logger": "com.unified.scheduler.service.SchedulingServiceImpl",
    "message": "Appointment confirmed successfully",
    "context": {
      "dealershipId": "1",
      "appointmentId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "referenceNumber": "APT-20261026-A1B2",
      "bayId": "1",
      "technicianId": "1"
    }
  }
  ```

### 6.2. Health Checks & Probes
- **`/actuator/health`**:
  - **Liveness Probe**: Confirms the JVM and Spring application context are healthy and running.
  - **Readiness Probe**: Verifies active connectivity to PostgreSQL 17 and successful execution of Flyway migrations before processing traffic.

---

## 7. Concurrency Control & Double-Booking Prevention

The Unified Service Scheduler enforces a strict **Zero Double-Booking Guarantee** across 4 core business dimensions:
1. **Service Bay**: A physical bay can accommodate at most one vehicle during any scheduled window.
2. **Technician**: A qualified technician can be assigned to at most one appointment during any scheduled window.
3. **Customer**: A customer cannot have multiple overlapping service appointments.
4. **Vehicle VIN**: A unique vehicle cannot be concurrently scheduled for multiple services at overlapping times.

### 7.1. Technical Implementation Architecture
1. **Multi-Entity Conflict Validation (Customer & Vehicle)**:
   - Prior to acquiring physical shop locks, the engine executes fail-fast temporal conflict checks against `AppointmentRepository`:
     ```sql
     SELECT COUNT(a) FROM Appointment a 
     WHERE a.customer = :customer 
       AND a.status IN ('CONFIRMED', 'IN_PROGRESS')
       AND (a.startTime < :endTime AND a.endTime > :startTime)
     ```
     ```sql
     SELECT COUNT(a) FROM Appointment a 
     WHERE a.vehicleVin = :vehicleVin 
       AND a.status IN ('CONFIRMED', 'IN_PROGRESS')
       AND (a.startTime < :endTime AND a.endTime > :startTime)
     ```
   - If either count > 0, the request is rejected with `ResourceUnavailableException` (HTTP 409 Conflict), preventing impossible human or physical vehicle double-scheduling.
2. **Pessimistic Row-Level Lock (`PESSIMISTIC_WRITE`)**:
   - The booking transaction begins by querying candidate bays and technicians with `FOR UPDATE` semantics.
   - Any concurrent transaction attempting to read or lock the same resource candidates is forced into a deterministic database queue.
3. **Temporal Resource Overlap Query**:
   ```sql
   SELECT a FROM Appointment a 
   WHERE a.serviceBay.id = :bayId 
     AND a.status IN ('CONFIRMED', 'IN_PROGRESS')
     AND (a.startTime < :endTime AND a.endTime > :startTime)
   ```
4. **Rollback & Conflict Response**:
   - If candidate bays or technicians are occupied by a preceding transaction, the transaction rolls back immediately and raises `ResourceUnavailableException`, resulting in HTTP 409 Conflict.
   - The double-booking count across bays, technicians, customers, and vehicles remains strictly zero.

---

## 8. GenAI Collaboration in the Design Phase

> [!NOTE]
> **Keyloop Technical Assessment Evaluation Dimension 3**: *AI Engineering & Verification - Your strategy for directing AI and your process for verifying, debugging, and owning the final solution.*

The design and architecture of The Unified Service Scheduler was conducted through an intentional, structured pair-programming collaboration between the lead engineer and Generative AI (Antigravity / Gemini). Rather than treating GenAI as a passive code generator, it was orchestrated as an architectural co-pilot under strict verification guardrails.

```mermaid
flowchart TD
    subgraph GenAI Orchestration & Verification Workflow
        P["Human Engineer:<br/>Strategic Direction"] --> A["GenAI: Architectural Proposals<br/>& Trade-off Analysis"]
        A --> B["Human Engineer:<br/>Critical Audit & Constraints"]
        B -->|Critique: Multi-service deadlocks| C["Composite Service Bundling<br/>& Deterministic Locks"]
        B -->|Approved: Modular standards| D["Generation of Modular Rules<br/>(DB, Java, Spring, Test)"]
        B -->|Critique: Synthetic UUIDs| H["Hybrid Key Architecture<br/>(BIGINT + UUID)"]
        D --> E["Autonomous Review Skills<br/>in .agents/skills/"]
        E --> F["Automated Verification<br/>(100 Test Cases)"]
        F --> G["Empirical Proof:<br/>Zero Double-Booking"]
    end
```

### 8.1. Strategic Directing & Constraint-Driven Prompting
- **Iterative Decomposition**: We directed the AI by decomposing the problem into discrete layers: requirements specification, modular coding rules, database schema constraints, concurrency control, and test automation.
- **Enforcing Granular Rules**: When initial proposals attempted to lump all engineering standards into a single broad document, we instructed the AI to decouple them into focused, authoritative rule sets:
  - `database-design-and-script-rules.md` (naming, UUID, TIMESTAMPTZ, range indexes, Flyway).
  - `java-coding-standards.md` (Java 21 records, immutability, pattern matching).
  - `spring-boot-architecture-rules.md` (strict layering, constructor injection, transactional scope).
  - `unit-and-integration-testing-rules.md` (testing pyramid, naming, concurrency verification).

### 8.2. Critical Evaluation: Single Service vs. Multi-Service Carts (Deadlock Prevention)
During appointment scheduling architecture design, the AI initially proposed allowing customers to select multiple arbitrary individual services within a single booking request (e.g., adding 3-4 separate service tasks to a cart). Through human architectural review, this was critically redirected:
- *Risk Identified*: Multi-resource reservations spanning multiple distinct services across concurrent transactions introduce high risk of **Distributed Deadlock** (e.g., Transaction 1 holds Bay A and waits for Tech B, while Transaction 2 holds Tech B and waits for Bay A).
- *Direction & Decision*: We rejected arbitrary multi-service cart reservations. Instead, combined dealership maintenance visits (e.g., 30,000-mile inspection combining oil change, brake inspection, and tire rotation) are modeled as standardized **Composite Services** with aggregated duration and the highest required skill code. This completely eliminates cyclical resource lock acquisition and prevents transaction deadlocks under high concurrency.

### 8.3. Autonomous Review Skills & Verification Guardrails
To enforce ownership and deterministic verification, we guided the AI to create **Workspace Review Skills** (`.agents/skills/`):
- `uss-code-reviewer`: Automatically audits Java 21 and Spring Boot code against architectural standards.
- `uss-db-script-reviewer`: Verifies schema DDL, constraints, and Flyway migration scripts.
- `uss-test-and-concurrency-verifier`: Executes the automated test suite and formally validates that concurrent race conditions result in zero double-booking.

### 8.4. Critical Evaluation: Synthetic UUIDs & Transition to Hybrid Key Architecture
During database migration and domain review, the human engineer critically challenged the AI's initial proposal of using UUIDs indiscriminately across all tables:
- *Critique Raised by Human Engineer*: In `V2__seed_data.sql`, the AI had generated synthetic, artificial UUIDs (`11111111-1111-1111-1111-111111111111`, `22222222-...`) to link seed data foreign keys. The human reviewer questioned why catalog entities with immutable natural business codes (`DLR-METRO`, `BRAKE_SERVICE`, `TECH-001`) required synthetic UUIDs at all, pointing out that this artificially overcomplicated data seeding and queries.
- *Collaborative Trade-off Analysis*:
  - Master/Catalog data is queried primarily within internal administrative or scheduling joins, has low churn, and benefits immensely from compact 8-byte `BIGINT IDENTITY` primary keys and 50% smaller composite B-Tree indexes on `appointments`.
  - Transactional appointment data is exposed directly over public APIs and absolutely requires `UUID` to prevent IDOR vulnerabilities and competitor enumeration of dealership transaction velocity.
- *Direction & Result*: The human engineer directed a refactoring to a **Pragmatic Hybrid Key Architecture**:
  - `dealerships`, `service_types`, `service_bays`, `technicians`, and `technician_skills` were converted to `BIGINT IDENTITY` PKs with natural business unique codes (`code VARCHAR`).
  - Seed SQL now resolves relational foreign keys cleanly using standard subqueries (`SELECT id FROM dealerships WHERE code = 'DLR-METRO'`).
  - `appointments` and `appointment_audit_logs` retain cryptographically random `UUID` PKs.
  - This pivot illustrates how experienced human engineering oversight steers AI away from dogmatic over-engineering ("UUID everywhere") toward pragmatic, high-performance production design.

All AI contributions and decisions are chronologically logged in [`docs/ai-collaboration-log.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/ai-collaboration-log.md) to ensure complete auditability and engineering ownership.
