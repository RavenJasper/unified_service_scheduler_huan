# Spring Boot Architecture & Implementation Rules

## 1. Strict Layered Architecture
Data and invocation flows strictly in one direction:
```
Client -> Controller -> Service -> Repository -> Database
```
- **Controller Layer (`controller`)**:
  - Handles HTTP requests, triggers validation, delegates to the Service layer, and maps responses to DTOs.
  - Must never contain core business logic, query Repositories directly, or manage transactions.
  - Must never return JPA Entities directly to API clients.
- **Service Layer (`service`)**:
  - Encapsulates scheduling algorithms, resource availability checks, and booking state transitions.
  - Defines transactional boundaries with `@Transactional`.
- **Repository Layer (`repository`)**:
  - Spring Data JPA interfaces; handles queries and database-level locks (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
- **Domain Layer (`domain`)**:
  - JPA Entities, Value Objects, Enums, and core domain invariants.

---

## 2. Dependency Injection
- **Mandatory Constructor Injection**:
  - All injected collaborators must be declared `private final`.
  - Initialized through constructors.
  - Field injection (`@Autowired` on private fields) is strictly forbidden.

---

## 3. Transaction Management
- **Explicit Boundaries**:
  - Use `@Transactional(readOnly = true)` for read-only operations (querying available slots, fetching appointment details) to optimize connection usage and Hibernate flush modes.
  - Use write `@Transactional` on booking, cancellation, and rescheduling operations.
  - Keep write transactions concise; avoid non-database operations (I/O, external network calls) inside transaction boundaries.

---

## 4. Input Validation
- All request DTO records must have Jakarta validation annotations (`@NotNull`, `@NotBlank`, `@Future`, `@Pattern`).
- Controllers must use `@Valid` on `@RequestBody` parameters.

---

## 5. Global Exception Handling & RFC 7807 Problem Details
- Centralized exception mapping using `@RestControllerAdvice` in `GlobalExceptionHandler`.
- Standardize all error responses on RFC 7807 using Spring 3's `ProblemDetail`:
  - `status`, `title`, `detail`, `instance`, and custom properties for field-level validation breakdowns.

---

## 6. JPA & Hibernate Performance
- All `@ManyToOne` and `@OneToMany` relationships must explicitly specify `fetch = FetchType.LAZY`.
- Prevent N+1 queries by utilizing `JOIN FETCH` in JPQL or `@EntityGraph`.
- Explicitly set `spring.jpa.open-in-view: false` in application configuration.

---

## 7. Query Sorting, Search Endpoints & Pagination Standards
- **Deterministic Read Sorting (Code first, then Name)**:
  - All read/query endpoints and repository finder methods must enforce deterministic sorting: **Code first (`code ASC`), then Name second (`name ASC`)**.
  - Canonical sort orders across domain models:
    - `Dealership`: `code ASC, name ASC`
    - `ServiceType`: `code ASC, name ASC`
    - `Technician`: `employeeCode ASC, lastName ASC, firstName ASC`
    - `ServiceBay`: `bayNumber ASC, name ASC`
    - `Appointment`: `startTime DESC, referenceNumber ASC`
- **API Pattern: Table Grid Search (Paged) vs Dropdown Lookup (Unpaged)**:
  - **Table Grid Search (`GET /api/v1/{resources}` & `GET /api/v1/{resources}/search`)**:
    - The root collection resource serves Frontend Table / Management screens.
    - Standardized on Spring Data `Pageable` (`page`, `size`, `sort`) and keyword/domain filtering (`query`, `dealershipId`, etc.).
    - Returns an immutable `PageResponseDto<T>` with pagination metadata (`totalElements`, `totalPages`, `isFirst`, `isLast`).
    - Applies `@PageableDefault(page = 0, size = 10, sort = {"code", "name"})`.
  - **Infinite Scroll Lightweight Dropdown Lookup (`GET /api/v1/{resources}/lookup`)**:
    - Dedicated endpoint specifically tailored for form `<select>` dropdowns and fast typeahead autocomplete widgets.
    - Uses ultra-compact `LookupOptionDto(id, code, label)` eliminating over-fetching and slashing payload size by 90%.
    - Employs **Infinite Scroll / Virtual Pagination** to handle data volume growth without cutting off matching records:
      - Returns `LookupPageResponseDto(items, page, limit, totalElements, hasMore)`.
      - Accepts `query` for typeahead narrowing, `page` (default 0), and `limit` (default 20, ceiling 100).
      - The `hasMore = (page + 1) * limit < totalElements` flag signals frontend dropdown components to trigger consecutive page requests as users scroll down.
  - **Transactional Integrity**:
    - Both search and lookup methods must be declared with `@Transactional(readOnly = true)` at implementation level to avoid Hibernate `LazyInitializationException` across relationship navigations.

### 8. Flexible Availability Query & Scheduling Strategy

- **Default Search Window**: When date parameters are omitted, availability queries standardly default to **1 week (7 days)** starting from current date (`LocalDate.now()` to `LocalDate.now().plusDays(6)`).
- **Maximum Search Ceiling**: Search range is capped at **31 days (1 month)**. Ranges exceeding 31 days must immediately reject with RFC 7807 `400 Bad Request` (`InvalidBookingException`).
- **Selective Time Precision (`desiredTime`)**: Clients can supply an optional `desiredTime` (`LocalTime`, e.g., `09:00:00`) to filter matching time slots across the entire date window.
- **Past-Slot Pruning**: For today's date, slots occurring prior to current dealership local time must be filtered out to prevent impossible past bookings.
- **Backward Compatibility**: A single `date` parameter (`date=YYYY-MM-DD`) remains supported for single-day targeted queries.

---

## 9. Disciplined Lombok Usage for JPA Entities
- **Scope Restriction**: Project Lombok is permitted for `@Getter` and `@Setter` on JPA Entities to eliminate repetitive accessor boilerplate while keeping domain models clean and readable.
- **Entity Identity & Equals/HashCode**:
  - When entity equality comparison is required (such as in `Customer`), use `@EqualsAndHashCode(of = "id")` strictly bounded to the primary key `id`.
  - Blanket/indiscriminate `@EqualsAndHashCode` across all fields is **strictly forbidden** (prevents mutable hash code bugs when fields change).
- **Strict Prohibitions**:
  - `@Data` is **strictly forbidden** on JPA entities (generates unwanted mutable `equals`, `hashCode`, and `toString` that break Hibernate entity identity semantics).
  - `@ToString` and relationship-inclusive `@EqualsAndHashCode` are **strictly forbidden** on JPA entities (triggers inadvertent loading of `@OneToMany`/`@ManyToOne` lazy proxies, causing `LazyInitializationException` or recursive stack overflows).
- **DTOs & Immutability**: All API request and response DTOs must remain pure immutable Java 21 `record`s, not Lombok classes.
- **Domain Methods**: Custom domain logic methods (e.g. `getFullName()`, `hasSkill()`, `reassignTechnician()`, `reschedule()`, `cancel()`) remain explicitly written on the entity.
