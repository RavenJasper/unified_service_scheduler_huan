# AI Engineering & Verification Rules

## 1. Intentional AI Governance
This project is engineered collaboratively between the human engineer and AI assistants. To ensure the highest level of craftsmanship and transparent ownership, the following rules apply:

1. **Constraint-Driven Prompting**:
   - Every architectural component is directed by specific rule files: Database Design Rules, Java Standards, Spring Boot Architecture, and Testing Standards.
   - AI outputs must never be accepted without deterministic verification.
2. **Automated Verification**:
   - Every feature must be verified through the automated testing suite (`mvn clean test`).
   - Concurrency safety must be empirically proven through multi-threaded tests.
3. **Audit Trail of AI Collaboration**:
   - Maintain a dedicated collaboration log in `docs/ai-collaboration-log.md` detailing architectural prompts, trade-off analysis, and debugging steps.

---

## 2. Evaluator Review Guide
Evaluators can inspect:
- **`docs/system-design.md`**: Section 6 "GenAI Assistance & Architectural Decision Rationale".
- **`docs/ai-collaboration-log.md`**: Step-by-step record of prompts, critiques, automated test verification, and debugging.
- **`docs/rules/`**: Dedicated modular rule files enforcing architectural and engineering discipline.
