# Java Coding Standards

## 1. Java Version & Modern Language Features (Java 21 LTS)
- **Java Records**:
  - Strictly use `record` for all DTOs (Request/Response), Value Objects, and Domain Events.
  - Avoid mutable class definitions with getters/setters for data transfer.
- **Local Variable Type Inference (`var`)**:
  - Use `var` when the type is obvious from the right-hand side initializer (e.g. `var appointment = new Appointment();`).
  - Avoid `var` when return types are ambiguous or complex.
- **Pattern Matching & Switch Expressions**:
  - Prefer exhaustive switch expressions returning values over nested `if-else` chains.
  - Leverage `instanceof` pattern matching.
- **Immutable Collections**:
  - Favor `List.of()`, `Set.of()`, `Map.of()`, or `List.copyOf()` for immutability.

---

## 2. Naming Conventions
- **Classes, Records, Interfaces, Enums**: `PascalCase` (e.g., `SchedulingService`, `AppointmentResponseDto`, `SkillCode`).
- **Methods & Variables**: `camelCase` (e.g., `bookAppointment`, `availableBays`, `startTime`).
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_SLOT_DURATION_MINUTES`).
- **Packages**: Flat, lowercase dotted names (e.g., `com.unified.scheduler.domain`).

---

## 3. Null Safety & Defensive Programming
- **No Null Returns**:
  - Single-entity query methods that may not find a result must return `Optional<T>`.
  - Collection query methods must return empty collections (`List.of()`), never `null`.
- **Constructor Validation**:
  - Validate non-null requirements using `Objects.requireNonNull(arg, "arg must not be null")`.

---

## 4. Exception Handling
- **Hierarchy**:
  - Custom domain exceptions must inherit from an unchecked `BusinessException` (extends `RuntimeException`).
  - Concrete exceptions:
    - `ResourceNotFoundException`: Maps to HTTP 404.
    - `ResourceUnavailableException`: Maps to HTTP 409 Conflict.
    - `InvalidBookingException`: Maps to HTTP 400 / 422.
- **No Swallowing Exceptions**:
  - Never leave catch blocks empty or use raw `e.printStackTrace()`.

---

## 5. Stream API & Lambdas
- Keep stream pipelines clear and readable; place intermediate operations (`filter`, `map`, `sorted`) on distinct lines.
- Avoid side-effects inside stream operators (`forEach`, `map`).
- Prefer database-level filtering over fetching all rows and filtering in memory.
