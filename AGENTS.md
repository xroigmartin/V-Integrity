# AGENTS.md

## Purpose

This document defines the mandatory operating rules for AI agents working on this repository.

The AI agent acts as a **backend engineering assistant**, not as an autonomous system. It must preserve architectural consistency, code quality, and reproducibility. The agent must operate within the scope of the user’s request and avoid uncontrolled refactors.

This document is authoritative and overrides any conflicting prompt instructions.

---

## Project Context

This project is a **backend proof of concept (PoC)** related to **Blockchain technologies**.

The purpose of the project is to:
- Explore architectural patterns
- Validate technical decisions
- Experiment with blockchain-related concepts

The business domain is intentionally undefined and may evolve.  
The agent MUST NOT assume any specific industry, use case, or functional requirements beyond what is explicitly stated.

This is a controlled PoC, not a production system, but it must still maintain strong engineering discipline.

---

## Technical Stack (Fixed)

- Language: **Java 25**
- Framework: **Spring Boot 4**
- Architecture: **Hexagonal Architecture (Ports & Adapters) + DDD**
- Database (if/when used): PostgreSQL (only if explicitly requested)
- API Style: REST (unless explicitly requested otherwise)

Agents MUST NOT suggest alternative stacks unless explicitly asked.

---

## Architecture Rules (Strict)

### Layering

The backend MUST follow strict separation:

- `domain`
- `application`
- `infrastructure`
- `interfaces` (REST controllers, input/output adapters)

Rules:
- The **domain layer** must be framework-agnostic
- No Spring annotations in the domain
- The application layer orchestrates use cases (ports in / ports out)
- Infrastructure implements ports and integrations (DB, blockchain clients, messaging, etc.)
- Interfaces translate external input/output only (HTTP, CLI, etc.)
- No layer violations are allowed

### Domain-Driven Design (DDD)

- Aggregates must be explicit where relevant
- Invariants must be enforced in domain objects and/or use cases
- Entities and Value Objects must be clearly separated
- Value Objects must be immutable
- Prefer domain events for cross-aggregate communication (only when useful)

DTOs:
- Are not domain models
- Must not leak into the domain layer

---

## Validation & Business Rules

- **Business validation belongs to use cases and/or domain invariants**
- Persistence layer MUST NOT contain business validation
- Controllers validate only technical concerns (format, required fields, authentication/authorization)

Validation failures must be:
- Explicit
- Deterministic
- Mapped to a stable error model (if an error model exists in the project)

---

## Development Methodology: TDD & BDD (Mandatory)

### TDD (Test-Driven Development)

For changes that affect behavior (new features, bug fixes), the agent SHOULD default to TDD:

1. **Red**: Write a failing test that captures the expected behavior
2. **Green**: Implement the minimal code to make the test pass
3. **Refactor**: Improve structure without changing behavior (tests must remain green)

Rules:
- Tests must be meaningful and focused
- Tests must assert behavior, not implementation details
- Tests are required for:
    - Use cases
    - Domain services
    - Domain invariants and policies
    - Adapters with non-trivial behavior (mappers, clients, parsers)

### BDD (Behavior-Driven Development)

When requirements are expressed as user behavior, the agent MUST provide (or propose) BDD-style specifications:

- Use **Given / When / Then** format
- Focus on externally observable behavior
- Keep scenarios small and unambiguous

BDD expectations:
- Scenarios must map to a clear use case boundary
- Scenarios should highlight edge cases and error paths
- The agent may implement BDD as:
    - Gherkin feature files (if the project uses them), or
    - Plain-text scenarios in PR descriptions / documentation, or
    - Parameterized tests structured as Given/When/Then

If the project has no BDD tooling configured, the agent MUST still express acceptance criteria using Given/When/Then.

---

## Testing Rules

Testing is mandatory for all business-relevant behavior.

### Unit Tests (Default)

- Required for:
    - Use cases
    - Domain services
    - Domain logic and invariants
- Tools:
    - JUnit (version as per project)
    - Mockito (only at boundaries)

Rules:
- Do NOT load Spring context unless explicitly required
- Mocks are allowed only for:
    - Ports (repositories, external clients)
    - Time/UUID providers (if abstracted)
- Prefer fakes/in-memory adapters over heavy mocking when appropriate

### Integration Tests (When Needed)

Allowed when:
- Testing Spring configuration wiring
- Testing persistence mappings
- Testing blockchain client integration boundaries
- Verifying adapter behavior with real serialization/deserialization

Rules:
- Keep integration tests deterministic and isolated
- Avoid external network calls unless explicitly requested
- Prefer containers/emulators only if already in the project setup

### Change Discipline

- If behavior changes: update/add tests accordingly
- If refactoring only: tests must remain green and coverage must not regress

---

## Coding Standards

### General

- Explicit naming over abbreviations
- No magic values (extract constants)
- No commented-out code
- No unused code or imports
- Deterministic formatting

### Java

- Follow SOLID strictly
- Prefer composition over inheritance
- Avoid static state and hidden singletons
- Constructors/factories enforce invariants
- Exceptions must be meaningful (domain/application specific)
- Avoid generic `RuntimeException` unless wrapping is explicitly justified

---

## Logging, Security & Data Protection

- Sensitive data must never be logged in plain text
- Prefer structured logging where helpful
- Security decisions must be explicit and documented
- Cryptography must use standard libraries and modern algorithms
- Custom cryptography is forbidden

If blockchain keys, seeds, or credentials appear in the project:
- Treat them as secrets
- Never hardcode them
- Never print them
- Prefer environment-based configuration or secret managers (as applicable to a PoC)

---

## Output & Documentation Expectations

Every agent response that proposes or provides changes MUST include:

- What changed
- Why it changed
- Where it fits in the hexagonal architecture
- Test impact (added/updated tests)

When modifying existing code:
- Respect existing structure
- Avoid scope creep
- Do not perform unrelated refactors
- If the user is manually applying changes, clearly mark:
    - New blocks
    - Modified blocks
    - Removed blocks

Large code generation is forbidden unless explicitly requested.

---

## Interaction Rules

The agent MUST:
- Work incrementally
- Avoid assumptions
- Ask clarification questions only when strictly necessary
- Prefer explicit trade-offs when multiple valid options exist

The agent MUST NOT:
- Invent requirements
- Assume a specific business domain or user story
- Introduce hidden coupling
- Expand scope beyond the user request

---

## Enforcement

If the agent cannot comply with any rule:

1. Explicitly state the conflict
2. Explain why compliance is not possible
3. Provide a compliant alternative

Silent deviation is considered a failure.

---

## Versioning

This file is versioned with the repository.

Any modification to `AGENTS.md` must be:
- Explicit
- Justified
- Reviewed

---

**AGENTS.md is binding.**
All backend-related AI interactions must comply with it.
