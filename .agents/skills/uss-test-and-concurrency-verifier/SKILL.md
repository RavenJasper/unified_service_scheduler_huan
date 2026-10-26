---
name: uss-test-and-concurrency-verifier
description: >-
  Verify test suites, execute automated tests, and validate multi-threaded concurrency
  stress tests to prove zero double-booking and >=85% test coverage for The Unified
  Service Scheduler against testing standards.
---

# USS Test & Concurrency Verifier Skill

This skill guides the execution and verification of test suites for **The Unified Service Scheduler (USS)** project, ensuring complete test pyramid coverage, regression prevention, and empirical proof of zero double-booking under race conditions.

---

## 1. When to Use This Skill
Activate this skill whenever:
- Reviewing test classes in `src/test/java/`.
- Verifying the implementation of unit, slice, integration, or concurrency tests.
- Running automated test suites via Maven and analyzing test execution logs.
- Validating compliance with [`docs/rules/unit-and-integration-testing-rules.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/rules/unit-and-integration-testing-rules.md).

---

## 2. Review Checklist

### A. Testing Pyramid Structure
- [ ] **Unit Tests (`SchedulingServiceTest`, `SlotCalculatorTest`)**:
  - Test pure business logic, boundary generation, and calculations in isolation.
  - Zero external I/O, no Spring Context startup. Fast execution.
- [ ] **Repository Slice Tests (`AppointmentRepositoryTest`)**:
  - Annotated with `@DataJpaTest`.
  - Validate custom queries, temporal overlap calculations, and `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- [ ] **API Integration Tests (`AppointmentControllerIntegrationTest`)**:
  - Annotated with `@SpringBootTest` and `@AutoConfigureMockMvc`.
  - Verify complete request-response lifecycles, HTTP statuses (201, 400, 404, 409), and RFC 7807 error structures.
- [ ] **Concurrency Stress Tests (`AppointmentConcurrencyTest`)**:
  - Multi-threaded execution using `ExecutorService` and `CountDownLatch` or `CompletableFuture.allOf()`.
  - 20-50 concurrent requests competing for the exact same remaining bay/technician slot.
  - Assertion: Exactly 1 request succeeds (201 Created); all others receive 409 Conflict; zero double-booking in database.

### B. Test Code Quality
- [ ] **Naming Conventions**:
  - Does every test follow `should{ExpectedBehavior}_when{StateUnderTest}()`?
  - Examples: `shouldBookAppointment_whenBayAndTechnicianAreAvailable()`, `shouldThrowConflict_whenAllBaysAreFullyBooked()`.
- [ ] **AAA Pattern**: Are Arrange, Act, and Assert clearly demarcated in each test method?
- [ ] **AssertJ Assertions**: Are expressive assertions used (`assertThat(...).isEqualTo(...)`)?

---

## 3. Automated Test Execution & Verification Procedure
Run the automated test suite using Java 21 and Maven:

```bash
JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" /opt/homebrew/bin/mvn clean test
```

### Verification Checks:
1. Verify that `BUILD SUCCESS` is achieved with 0 failures and 0 errors.
2. Verify that `AppointmentConcurrencyTest` ran and passed with 1 confirmed booking and N-1 rejected conflicts.
3. Check code coverage reports or verify that all critical service methods and edge cases are exercised.

---

## 4. Verification Output Format
When executing this skill, output the verification report in this structured format:

```markdown
### Test & Concurrency Verification Report
- **Execution Status**: [PASSED / FAILED]
- **Tests Run**: Total: [X], Failures: [0], Errors: [0], Skipped: [0]

#### Concurrency Stress Test Results:
- Threads Dispatched: [X]
- Successful Bookings: [1] (Expected: exactly 1)
- Conflict Rejections (409): [X - 1]
- Double-Booking Count: [0] (PASSED)

#### Coverage & Edge Case Assessment:
- Unit Test Coverage: [X%]
- Happy Path Verified: [Yes/No]
- Error Paths Verified (400, 404, 409): [Yes/No]
- Verdict: [APPROVED / CHANGES_REQUIRED]
```
