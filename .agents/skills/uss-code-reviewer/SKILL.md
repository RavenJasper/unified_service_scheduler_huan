---
name: uss-code-reviewer
description: >-
  Review Java 21 and Spring Boot 3 code for The Unified Service Scheduler (USS)
  against project engineering standards, clean architecture, transactional boundaries,
  RFC 7807 problem details, and concurrency safety.
---

# USS Code Reviewer Skill

This skill guides the review and audit of Java 21 and Spring Boot 3 source code within **The Unified Service Scheduler (USS)** project.

---

## 1. When to Use This Skill
Activate this skill whenever:
- Reviewing newly created or modified Java classes, controllers, services, repositories, or DTOs.
- Auditing code quality before merging or completing a feature phase.
- Validating adherence to [`docs/rules/java-coding-standards.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/rules/java-coding-standards.md) and [`docs/rules/spring-boot-architecture-rules.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/rules/spring-boot-architecture-rules.md).

---

## 2. Review Checklist

### A. Java 21 Standards & Idioms
- [ ] **DTO Immutability**: Are all Request and Response DTOs declared as Java `record`s? (No mutable classes with getter/setter for data transfer).
- [ ] **Type Inference**: Is `var` used appropriately for obvious initializers, and avoided when return types are ambiguous?
- [ ] **Pattern Matching**: Are switch expressions and pattern matching used where applicable?
- [ ] **Null Safety**:
  - Do query methods return `Optional<T>` for potentially missing single results?
  - Do collection methods return empty collections (`List.of()`), never `null`?
  - Does constructor validation use `Objects.requireNonNull()`?
- [ ] **Exception Design**:
  - Do custom business exceptions inherit from `BusinessException` (extending `RuntimeException`)?
  - Are exceptions descriptive, containing identifiers and context?
  - Are exceptions never silently swallowed or logged without context?

### B. Spring Boot 3 & Architecture Layering
- [ ] **Strict Layer Separation**:
  - Controller -> Service -> Repository -> Database.
  - Does the Controller delegate immediately to Service without containing business logic?
  - Does the Controller NEVER call Repositories directly?
  - Are JPA Entities NEVER returned directly in API responses (always mapped to DTOs)?
- [ ] **Dependency Injection**:
  - Are all injected dependencies declared as `private final` fields?
  - Is Constructor Injection strictly used? (No `@Autowired` on private fields).
- [ ] **Transaction Management**:
  - Is `@Transactional(readOnly = true)` used on read-only service methods?
  - Is write `@Transactional` applied with minimal scope on mutating methods?
  - Are long-running I/O or external calls kept outside of write transactions?
- [ ] **Validation**:
  - Are Jakarta validation annotations (`@NotNull`, `@NotBlank`, `@Future`) present on request DTO fields?
  - Does the Controller method have `@Valid` on `@RequestBody`?
- [ ] **Error Handling**:
  - Are all exceptions intercepted by `@RestControllerAdvice` in `GlobalExceptionHandler`?
  - Are responses formatted according to **RFC 7807** (`ProblemDetail`) with appropriate HTTP status codes (400, 404, 409, 422)?
- [ ] **JPA Best Practices**:
  - Are all `@ManyToOne` and `@OneToMany` relationships explicitly marked `fetch = FetchType.LAZY`?
  - Are queries using `JOIN FETCH` or `@EntityGraph` to prevent N+1 select problems?

---

## 3. Review Output Format
When executing this skill, output the review findings using this structured markdown format:

```markdown
### Code Review Summary: [Class/Package Name]
- **Overall Verdict**: [PASSED / CHANGES_REQUESTED]
- **Files Inspected**: list of files

#### Critical Issues (Must Fix):
- `File.java:Line`: Description of issue and required fix.

#### Warnings & Best Practice Suggestions:
- `File.java:Line`: Explanation and suggestion.

#### Compliance Score:
- Java 21 Standards: [X/10]
- Spring Boot Architecture: [X/10]
- Security & Concurrency: [X/10]
```
