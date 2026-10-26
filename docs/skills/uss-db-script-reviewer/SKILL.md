---
name: uss-db-script-reviewer
description: >-
  Review PostgreSQL database schema design, Flyway migration scripts (V1, V2),
  DDL constraints, indexing strategies, and pessimistic locking queries for
  The Unified Service Scheduler against database engineering rules.
---

# USS Database & Script Reviewer Skill

This skill guides the audit of PostgreSQL database schema design, Flyway SQL migrations, column specifications, and database-level concurrency mechanisms in **The Unified Service Scheduler (USS)** project.

---

## 1. When to Use This Skill
Activate this skill whenever:
- Reviewing Flyway SQL migration files in `src/main/resources/db/migration/` (`V1__init_schema.sql`, `V2__seed_data.sql`).
- Auditing database entities, custom JPQL/native queries, and locking configurations in `repository/`.
- Verifying adherence to [`docs/rules/database-design-and-script-rules.md`](file:///Users/jaspernguyen/Projects/ADRIAN/ITV_ALTER/KL/1_test_s_a/docs/rules/database-design-and-script-rules.md).

---

## 2. Review Checklist

### A. Naming Conventions & Schema Structure
- [ ] **Table Names**: Are tables named in lowercase `snake_case` in plural form (e.g., `dealerships`, `service_bays`, `technicians`, `appointments`)?
- [ ] **Primary Keys**:
  - Is the primary key column always named `id`?
  - Is the data type `UUID` with default generation `gen_random_uuid()`?
  - Is the constraint named following `pk_{table_name}`?
- [ ] **Foreign Keys**:
  - Are foreign key columns named `{singular_referenced_table}_id`?
  - Is the constraint named following `fk_{table}_{referenced_table}_{column}`?
  - Does every foreign key column have a corresponding database index?
- [ ] **Column Specifications & Types**:
  - Are string columns bounded? (No unbounded `VARCHAR`).
  - Vehicle VIN: `VARCHAR(17)`.
  - Reference Number: `VARCHAR(32)`.
  - Are all timestamp columns defined as `TIMESTAMP WITH TIME ZONE` (`TIMESTAMPTZ`)? (Never timezone-naive `TIMESTAMP`).
  - Are enums stored as `VARCHAR(30)` with a database `CHECK CONSTRAINT` on allowed values?
- [ ] **Mandatory Audit Columns**:
  - Do all core/transactional tables contain:
    - `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
    - `updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
    - `version INT NOT NULL DEFAULT 0` (for optimistic locking)

### B. Indexing & Concurrency Protection
- [ ] **Temporal Range Overlap Query Indexing**:
  - Does the schema include composite B-Tree indexes to accelerate overlap checks `(start_time < :endTime AND end_time > :startTime)`?
    - `CREATE INDEX idx_appointments_bay_time ON appointments (service_bay_id, start_time, end_time);`
    - `CREATE INDEX idx_appointments_tech_time ON appointments (technician_id, start_time, end_time);`
    - `CREATE INDEX idx_appointments_dealer_time ON appointments (dealership_id, start_time, end_time);`
- [ ] **Pessimistic Locking Semantics**:
  - Do reservation queries in `AppointmentRepository` employ `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) to prevent race conditions during concurrent bookings?
  - Are active status filters applied so cancelled bookings do not block slots?

### C. Flyway Migration Standards
- [ ] **File Naming**: Strictly follows `V{version}__{description}.sql` with two underscores.
- [ ] **Separation of Concerns**: Schema DDL in `V1`, seed data in `V2`.
- [ ] **Idempotence & Safety**: Uses `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS`.
- [ ] **Realistic Seed Data**: Seed data reflects valid dealerships, multi-stall bays, technicians with distinct skill codes, and services with realistic durations.

---

## 3. Review Output Format
When executing this skill, output the review findings using this structured markdown format:

```markdown
### Database & Migration Review Summary: [Migration/Repository Name]
- **Overall Verdict**: [PASSED / CHANGES_REQUESTED]
- **Files Inspected**: list of files

#### Schema & DDL Findings:
- `V1__init_schema.sql:Line`: Findings on data types, constraints, or PK/FK naming.

#### Indexing & Concurrency Findings:
- `Repository/SQL:Line`: Findings on indexes, locking queries, or race condition risks.

#### Compliance Score:
- Column & Data Type Precision: [X/10]
- Indexing & Query Efficiency: [X/10]
- Concurrency & Locking Safety: [X/10]
```
