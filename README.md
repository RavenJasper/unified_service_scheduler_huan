# The Unified Service Scheduler (USS)

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3+-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-red.svg)](https://flywaydb.org/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20(Swagger)-brightgreen.svg)](https://swagger.io/)

---

## 1. Project Overview
**The Unified Service Scheduler (USS)** is an intelligent automotive service appointment scheduling backend operating within the **Ownership** domain. It replaces fragmented, manual, and phone-based booking workflows with a production-grade, highly reliable, and resource-constrained scheduling engine designed for dealership service networks.

### Core Capabilities:
1. **Multi-Resource Constrained Scheduling**:
    - An appointment can only be confirmed when **both** a physical **Service Bay** AND a **Qualified Technician** (holding requisite certifications for the requested service type) are simultaneously available for the full duration of the service.
2. **4-Dimensional Conflict & Zero Double-Booking Guarantee**:
    - Enforces simultaneous availability across **Bay + Technician + Customer + Vehicle VIN**. Employs database-level pessimistic locking (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`), ACID transaction boundaries, and composite indexes to guarantee that concurrent booking requests targeting the exact same slot result in strictly one confirmation and zero double-bookings.
3. **Confirmed Persistence & Lifecycle Audit**:
    - Persists immutable appointment records associating customer, vehicle VIN, dealership, service type, allocated bay, and assigned technician. All lifecycle state transitions (`CONFIRMED`, `CANCELLED`, `RESCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `REASSIGNED`) are recorded in a permanent audit trail queried via `GET /api/v1/appointments/{id}/audit-logs`.
4. **End-to-End Observability**:
    - Instrumented with Spring Boot Actuator health checks, Micrometer Prometheus metrics, and structured JSON logging with MDC `traceId` correlation.

---

## 2. Technology Stack & Justifications

| Component | Technology | Rationale |
| :--- | :--- | :--- |
| **Language & Runtime** | **Java 21 (LTS)** | Leverages Java Records for immutable DTOs, Pattern Matching, virtual-thread readiness, and enterprise stability. |
| **Framework** | **Spring Boot 3.3+** | Robust ecosystem for REST APIs, declarative transaction management, Spring Data JPA, and Actuator metrics. |
| **Database** | **PostgreSQL 17** | ACID-compliant relational storage supporting row-level pessimistic locks (`FOR UPDATE`), `TIMESTAMPTZ`, and Hybrid Key Architecture (BIGINT identity for catalogs + UUID for appointments). |
| **Schema Migration** | **Flyway** | Version-controlled, immutable database migrations (`V1__init_schema.sql`, `V2__seed_data.sql`). |
| **API Contract & Docs** | **Springdoc OpenAPI 3.0** | Interactive Swagger UI documentation available at `/swagger-ui.html`. |
| **API Verification Client** | **Bruno** | Git-friendly, file-based API collection stored directly in `bruno/` for rapid testing and verification. |
| **Testing** | **JUnit 5, Mockito, AssertJ, MockMvc** | Multi-tier testing: Unit tests, JPA repository slice tests, API integration tests, and multi-threaded concurrency stress tests. |

---

## 3. Project Directory Structure

```text
1_test_s_a/
├── .agents/skills/                         # Antigravity Workspace Review Skills
│   ├── uss-code-reviewer/                  # Java 21 & Spring Boot 3 code review skill
│   ├── uss-db-script-reviewer/             # PostgreSQL schema & Flyway migration audit skill
│   └── uss-test-and-concurrency-verifier/  # Test suite & concurrency stress test verification skill
│
├── docs/                                   # Architectural, requirement, and standard documents
│   ├── requirements.md                     # Software Requirements Specification
│   ├── implementation-plan.md              # Detailed implementation plan (Phases 1-9)
│   ├── assumption-and-tradeoff.md          # Architectural assumptions & trade-off analysis
│   ├── system-design.md                    # System Design Document (Part 1 Deliverable)
│   ├── ai-collaboration-log.md             # AI engineering log & verification records
│   ├── skills/                             # Mirrored review skills documentation
│   └── rules/                              # Specialized engineering rules & standards
│       ├── database-design-and-script-rules.md       # Database, column, index & Flyway rules
│       ├── java-coding-standards.md                  # Java 21 coding standards
│       ├── spring-boot-architecture-rules.md         # Spring Boot architecture & DI rules
│       ├── unit-and-integration-testing-rules.md     # Testing pyramid & concurrency test rules
│       └── ai-engineering-and-verification-rules.md    # AI governance & audit rules
│
├── bruno/                                  # Bruno API Collection for endpoint verification
│   ├── bruno.json                          # Collection configuration
│   ├── environments/                       # Environments (local.bru)
│   ├── 01-catalogs/                        # Dealerships & Service Types queries
│   ├── 02-availability/                    # Real-time slot discovery (available-slots)
│   ├── 03-appointments/                    # Booking, Query, Reschedule & Cancel flows
│   ├── 04-conflict-testing/                # Conflict tests (409 Conflict) & validation errors
│   └── 05-observability/                   # Healthcheck & Prometheus Metrics
│
├── src/
│   ├── main/
│   │   ├── java/com/unified/scheduler/
│   │   │   ├── controller/                 # REST API Controllers
│   │   │   ├── service/                    # Business Logic & Scheduling Engine
│   │   │   ├── domain/                     # JPA Entities, Enums, Value Objects
│   │   │   ├── repository/                 # Spring Data JPA Repositories (Locking queries)
│   │   │   ├── dto/                        # Request / Response Records
│   │   │   ├── exception/                  # Domain Exceptions & RFC 7807 Handler
│   │   │   └── config/                     # OpenAPI, Observability & Auditing Config
│   │   └── resources/
│   │       ├── application.yml             # Main configuration (PostgreSQL, Flyway, Actuator)
│   │       ├── application-test.yml        # Test configuration (H2 PostgreSQL mode)
│   │       ├── logback-spring.xml          # Structured JSON Logging
│   │       └── db/migration/               # Flyway SQL Migrations
│   │           ├── V1__init_schema.sql     # DDL: Tables, Hybrid PKs, Constraints, Temporal Indexes
│   │           ├── V2__seed_data.sql       # DML: Central Metro Dealership, Bays, Technicians, Services
│   │           ├── V3__seed_westside_dealership.sql # DML: Westside Toyota Dealership, Bays & Technicians
│   │           └── V4__create_customers_table.sql   # DDL & DML: Dedicated Customer master data table
│   └── test/                               # Unit, Slice, Integration & Concurrency Test Suites
│
├── pom.xml                                 # Maven project descriptor
├── README.md                               # Project documentation entry point
└── .gitignore                              # Git ignore configuration
```

---

## 4. Documentation & Engineering Rules Index

### Requirements & Architecture:
- 📋 **Requirements Specification**: [`docs/requirements.md`](docs/requirements.md)
- 🗺️ **Implementation Plan**: [`docs/implementation-plan.md`](docs/implementation-plan.md)
- 🚀 **Future Roadmap**: [`docs/future-roadmap.md`](docs/assumption-and-tradeoff.md)
- 🏗️ **System Design Document**: [`docs/system-design.md`](docs/system-design.md)

### Specialized Engineering Rules:
1. 🗄️ **Database Design & Script Rules**: [`docs/rules/database-design-and-script-rules.md`](docs/rules/database-design-and-script-rules.md)  
   *(Table & column naming, Hybrid PKs (BIGINT/UUID), TIMESTAMPTZ, Composite B-Tree Indexes, Flyway migrations, Locking)*
2. ☕ **Java 21 Coding Standards**: [`docs/rules/java-coding-standards.md`](docs/rules/java-coding-standards.md)  
   *(Java Records for DTOs, Immutability, Pattern matching, Null safety, Domain exceptions)*
3. 🍃 **Spring Boot Architecture Rules**: [`docs/rules/spring-boot-architecture-rules.md`](docs/rules/spring-boot-architecture-rules.md)  
   *(Strict layered architecture, Constructor injection, @Transactional boundaries, RFC 7807 Problem Details)*
4. 🧪 **Testing & Verification Rules**: [`docs/rules/unit-and-integration-testing-rules.md`](docs/rules/unit-and-integration-testing-rules.md)  
   *(4-tier testing pyramid, Mockito, MockMvc, Multi-threaded concurrency stress test, >= 85% coverage)*
5. 🤖 **AI Engineering & Verification Rules**: [`docs/rules/ai-engineering-and-verification-rules.md`](docs/rules/ai-engineering-and-verification-rules.md)  
   *(Constraint-driven prompting, deterministic automated verification, audit log maintenance)*

### Custom Review & Verification Skills:
The workspace includes dedicated Antigravity review skills located in `.agents/skills/` (and mirrored in `docs/skills/`):
- 🔍 **`uss-code-reviewer`** ([`docs/skills/uss-code-reviewer/SKILL.md`](docs/skills/uss-code-reviewer/SKILL.md)): Audits Java 21 classes, Spring Boot layered boundaries, DI, transaction scope, and RFC 7807 error responses.
- 🗄️ **`uss-db-script-reviewer`** ([`docs/skills/uss-db-script-reviewer/SKILL.md`](docs/skills/uss-db-script-reviewer/SKILL.md)): Audits PostgreSQL schemas, Flyway migrations, column constraints, composite range indexes, and pessimistic locking queries.
- 🧪 **`uss-test-and-concurrency-verifier`** ([`docs/skills/uss-test-and-concurrency-verifier/SKILL.md`](docs/skills/uss-test-and-concurrency-verifier/SKILL.md)): Executes and verifies automated test suites (`mvn clean test`), asserting >=85% coverage and validating zero double-booking under multi-threaded concurrency.

#### How to Use the Skills with Antigravity:
You can trigger these skills directly in conversation to evaluate the implementation against the engineering standards:

1. **Reviewing Java & Spring Boot Code**:
   > *"Use the `uss-code-reviewer` skill to audit the implementation in `com.unified.scheduler.service`."*
    - *Result*: The agent inspects constructor injection, record DTO immutability, `@Transactional` scopes, and RFC 7807 problem details, returning an itemized compliance score.

2. **Auditing Database Schemas & Migrations**:
   > *"Activate `uss-db-script-reviewer` to audit `V1__init_schema.sql` and `V2__seed_data.sql`."*
    - *Result*: The agent checks for Hybrid primary keys (BIGINT/UUID), `TIMESTAMPTZ` datatypes, composite B-tree indexes for range overlaps, and row-level locking queries.

3. **Verifying Automated Tests & Concurrency Safety**:
   > *"Run `uss-test-and-concurrency-verifier` to execute tests and prove zero double-booking."*
    - *Result*: The agent executes `mvn clean test`, analyzes the multi-threaded concurrency test logs (verifying exactly 1 winner out of N parallel requests), and outputs a formal verification certificate.

> [!TIP]
> **Pre-Generated Review Reports**: All three review reports are persisted in [`docs/reviews/`](docs/reviews/README.md) for direct inspection.

---

## 5. Database Configuration & Flyway Migration Guide

### Profiles & Connection Settings
The application supports seamless database profiles defined in `src/main/resources/application.yml`:

| Profile | Target Database | Description |
| :--- | :--- | :--- |
| **`default` / `dev`** | PostgreSQL 17 | Connects to `jdbc:postgresql://localhost:5432/service_scheduler`. Runs Flyway migrations automatically upon startup. |
| **`test`** | H2 (Postgres Mode) | Self-contained in-memory database with PostgreSQL dialect. Runs Flyway migrations during `mvn test`. |
| **`prod`** | PostgreSQL (Managed/Remote) | Configured via standard environment variables without code modification. |

### Environment Variables for Custom / Existing PostgreSQL:
If you have an existing PostgreSQL instance, you can override database connection properties via environment variables or CLI flags:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://<HOST>:<PORT>/<DATABASE_NAME>"
export SPRING_DATASOURCE_USERNAME="<USERNAME>"
export SPRING_DATASOURCE_PASSWORD="<PASSWORD>"
```

### Running Flyway Migrations

#### Option 1: Automatic Migration on Spring Boot Startup (Recommended)
Spring Boot is pre-configured with `spring.flyway.enabled=true`. Whenever the application starts with a connected PostgreSQL database, Flyway automatically inspects `src/main/resources/db/migration/` and applies pending migrations sequentially:
1. `V1__init_schema.sql`: Creates core tables, hybrid keys (BIGINT identity for catalogs, UUID for appointments), foreign key constraints, and temporal composite indexes.
2. `V2__seed_data.sql`: Seeds reference Central Metro dealership, service bays, technicians with skill sets, and standard service types.
3. `V3__seed_westside_dealership.sql`: Seeds Westside Toyota branch with dedicated certified technicians and bays.
4. `V4__create_customers_table.sql`: Introduces dedicated `customers` master table (`id BIGINT IDENTITY`, `customer_code`, `email`, `phone`) and enforces referential integrity with `appointments.customer_id`.

Command:
```bash
mvn spring-boot:run
```

#### Option 2: Standalone Migration via Flyway Maven Plugin
If you wish to migrate an existing database without starting the web server:
```bash
mvn flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/service_scheduler \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres
```

#### Option 3: Direct Execution via `psql` (DBA Manual Run)
If your database permissions restrict runtime DDL execution, you can execute the raw SQL migration scripts directly:
```bash
psql -h localhost -p 5432 -U postgres -d service_scheduler -f src/main/resources/db/migration/V1__init_schema.sql
psql -h localhost -p 5432 -U postgres -d service_scheduler -f src/main/resources/db/migration/V2__seed_data.sql
psql -h localhost -p 5432 -U postgres -d service_scheduler -f src/main/resources/db/migration/V3__seed_westside_dealership.sql
psql -h localhost -p 5432 -U postgres -d service_scheduler -f src/main/resources/db/migration/V4__create_customers_table.sql
```

#### Checking Migration History in PostgreSQL:
Verify applied migrations in the database console:
```sql
SELECT installed_rank, version, description, type, execution_time, success 
FROM flyway_schema_history 
ORDER BY installed_rank;
```

---

## 6. Comprehensive REST API Reference

| Domain | HTTP Method | Endpoint | Key Parameters / Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Catalogs** | `GET` | `/api/v1/dealerships` | None | List active dealerships (compact) |
| | `GET` | `/api/v1/dealerships/search` | `query`, `city`, `page`, `size` | Paginated search of dealerships |
| | `GET` | `/api/v1/dealerships/{id}` | Path `{id}` | Retrieve single dealership by ID |
| | `GET` | `/api/v1/service-types` | None | List active service types (compact) |
| | `GET` | `/api/v1/service-types/search` | `query`, `skillCategory`, `page`, `size` | Paginated search of service types |
| | `GET` | `/api/v1/service-types/{id}` | Path `{id}` | Retrieve single service type by ID |
| | `GET` | `/api/v1/technicians` | `dealershipId`, `skillCode` | List technicians with certifications |
| | `GET` | `/api/v1/technicians/search` | `query`, `dealershipId`, `skillCode`, `page`, `size` | Paginated search of technicians |
| | `GET` | `/api/v1/technicians/{id}` | Path `{id}` | Retrieve single technician by ID |
| | `GET` | `/api/v1/service-bays` | `dealershipId` | List operational hydraulic lift bays |
| | `GET` | `/api/v1/service-bays/search` | `dealershipId`, `page`, `size` | Paginated search of service bays |
| | `GET` | `/api/v1/service-bays/{id}` | Path `{id}` | Retrieve single service bay by ID |
| | `GET` | `/api/v1/customers` | None | List active customer profiles |
| | `GET` | `/api/v1/customers/search` | `query`, `page`, `size` | Paginated search of customers |
| | `GET` | `/api/v1/customers/{id}` | Path `{id}` | Retrieve customer profile by ID |
| | `GET` | `/api/v1/customers/code/{code}` | Path `{code}` (e.g. `CUST-001`) | Lookup customer by natural code |
| **Availability** | `GET` | `/api/v1/schedules/available-slots` | `dealershipId`, `serviceTypeId`, `startDate`, `endDate`, `desiredTime` | Real-time calendar grid with `availableBays` & `availableTechnicians` |
| **Appointments** | `POST` | `/api/v1/appointments` | `BookingRequestDto` | Atomically reserve slot (enforces 4-way Bay + Tech + Customer + VIN non-overlap) |
| | `POST` | `/api/v1/appointments/{id}/reschedule` | `RescheduleRequestDto` | Reschedule confirmed appointment to a new slot |
| | `POST` | `/api/v1/appointments/{id}/reassign` | `ReassignTechnicianRequestDto` | Reassign appointment to another qualified technician in same dealership |
| | `POST` | `/api/v1/appointments/{id}/cancel` | `CancellationRequestDto` | Cancel appointment with audit reason; immediately liberates resources |
| | `GET` | `/api/v1/appointments/{id}` | Path `{id}` | Retrieve complete appointment record by UUID |
| | `GET` | `/api/v1/appointments/reference/{ref}` | Path `{ref}` (e.g. `APP-20261101-A1B2C3`) | Lookup appointment by human-readable reference number |
| | `GET` | `/api/v1/appointments` | `query`, `dealershipId`, `customerId`, `technicianId`, `status`, `page`, `size` | Workshop table search with cross-dealership technician validation |
| | `GET` | `/api/v1/appointments/{id}/audit-logs` | Path `{id}` | Chronological immutable audit trail (BOOKED, RESCHEDULED, REASSIGNED, CANCELLED) |
| **Observability** | `GET` | `/actuator/health` | None | Liveness & PostgreSQL readiness probes |
| | `GET` | `/actuator/prometheus` | None | High-cardinality Micrometer metrics for Prometheus |

---

## 7. Run App & Verification

### Prerequisites:
- **Java**: OpenJDK 21 LTS
- **Maven**: 3.9+
- **PostgreSQL**: 17 (running locally on port 5432 or configured in `application.yml`)

### Run Automated Tests (Unit, Integration & Concurrency):
```bash
mvn clean test
```
*Note: The automated test profile uses an in-memory database configured in PostgreSQL compatibility mode, allowing the entire test suite to execute independently with zero external setup.*

**Current Test Metrics**:
- **Total Tests**: **100/100 passing (100% Green, 0 Failures, 0 Errors, 0 Skipped)**
- **JaCoCo Line Coverage**: **92.87%** (Target: $\ge 85\%$)
- **JaCoCo Instruction Coverage**: **89.98%**
- **Concurrency Verification**: Empirically proven **0 double-bookings** across 20 simultaneous threads racing for 1 exclusive resource slot.

### Start the Application:
```bash
mvn spring-boot:run
```

### Verify Endpoints Using Bruno Collection:
1. Open the **Bruno** API client application.
2. Select **Open Collection** and navigate to the `bruno/` directory in this project.
3. Select the **`local`** environment from the top-right dropdown.
4. Execute requests sequentially across folders `01-catalogs` through `05-observability`.
    - The *Book Appointment* request automatically captures `id` and `referenceNumber` into environment variables for downstream retrieval, rescheduling, and cancellation testing.

### Interactive API Documentation (Swagger UI):
Once the server is running, explore and test endpoints via Swagger UI:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 8. AI Collaboration Narrative

### Strategy for Directing AI
This system was engineered through a disciplined pair-programming partnership between the lead software engineer and Generative AI (Google Antigravity / Gemini). Rather than permitting unconstrained code generation, the AI was governed via **constraint-driven prompting**:
- **Granular Decomposition**: We mandated independent, specialized engineering rule files (`database-design-and-script-rules.md`, `java-coding-standards.md`, `spring-boot-architecture-rules.md`, `unit-and-integration-testing-rules.md`) rather than allowing broad, generic guidelines.
- **Architectural Pruning & Deadlock Prevention**: When the AI initially proposed allowing arbitrary multi-service cart reservations within a single booking, we identified severe concurrency deadlock risks between competing transactions (where Request 1 locks Bay A waiting for Tech B, while Request 2 locks Tech B waiting for Bay A). We steered the architecture to bundle multi-service visits into standardized **Composite Services**, eliminating cyclical lock contention completely.

### Verification, Refining & Quality Assurance Process
To establish full ownership and ensure verified code quality:
1. **Workspace Review Skills**: We directed the creation of custom Antigravity review skills in `.agents/skills/` (`uss-code-reviewer`, `uss-db-script-reviewer`, `uss-test-and-concurrency-verifier`) to automate code inspection against project standards.
2. **Deterministic Automated Verification**: Every feature layer is validated through automated testing suites (`mvn clean test`). Concurrency race-condition safety is empirically proven through multi-threaded stress tests simulating 20-50 simultaneous requests against a single slot.
3. **Audit Trail**: Every architectural prompt, decision trade-off, and review iteration is chronologically recorded in [`docs/ai-collaboration-log.md`](docs/ai-collaboration-log.md).
