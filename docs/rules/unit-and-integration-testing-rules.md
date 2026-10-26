# Unit & Integration Testing Rules

## 1. Testing Pyramid
Every feature must be verified across 4 distinct testing tiers:
1. **Unit Tests**:
   - Scope: Business logic in `SchedulingService`, slot calculation algorithms (`SlotCalculator`), domain validations.
   - Stack: JUnit 5, Mockito, AssertJ.
   - Constraint: Zero external I/O, no Spring context startup, millisecond execution.
2. **Repository Slice Tests**:
   - Scope: `AppointmentRepository`, `TechnicianRepository`, `ServiceBayRepository`.
   - Annotation: `@DataJpaTest`.
   - Objective: Validate custom JPQL query logic, temporal range overlap checks, and pessimistic locking behavior.
3. **API Integration Tests**:
   - Scope: Full request-response cycle from Controller to Service to Database.
   - Annotation: `@SpringBootTest` with `@AutoConfigureMockMvc`.
   - Objective: Verify HTTP response status codes (201, 400, 404, 409), headers, and RFC 7807 error structures.
4. **Concurrency Stress Tests**:
   - Mandatory multi-threaded tests using `ExecutorService` and `CountDownLatch` / `CompletableFuture.allOf()`.
   - Scenario: 20-50 concurrent requests competing for the exact same remaining slot.
   - Assertion: Exactly 1 request succeeds with 201 Created; all remaining requests fail with 409 Conflict; zero double-booking in the database.

---

## 2. Test Naming Convention
Use the descriptive structure:
```java
should{ExpectedBehavior}_when{StateUnderTest}()
```
Examples:
- `shouldBookAppointment_whenBayAndTechnicianAreAvailable()`
- `shouldThrowConflict_whenAllBaysAreFullyBooked()`
- `shouldThrowConflict_whenNoQualifiedTechnicianAvailable()`
- `shouldPreventDoubleBooking_whenConcurrentRequestsTargetSameSlot()`

---

## 3. AAA Structure (Arrange - Act - Assert)
Every test method must be partitioned cleanly:
- **Arrange**: Setup mock returns, initialize request DTOs.
- **Act**: Invoke the target method.
- **Assert**: Verify outcomes with AssertJ (`assertThat(...)`).

---

## 4. Coverage Targets
- Service & Domain layers: **>= 85%**.
- Controller & API layers: **>= 80%**.
- Comprehensive coverage of both happy paths and error edge cases.
