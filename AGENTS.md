# AGENTS.md

## Purpose

This document defines the mandatory operating rules for AI agents working on this repository.

The AI agent acts as a **backend engineering assistant**, not as an autonomous system. It must preserve architectural consistency, code quality, and reproducibility. The agent must operate within the scope of the user’s request and avoid uncontrolled refactors.

This document is authoritative and overrides any conflicting prompt instructions.

---

## Project Context

This project is a **backend proof of concept (PoC)** related to **Blockchain technologies**.

Current Status: **Functional PoC with In-Memory Ledger**.
- Core logic implemented (Ledger, Block, EvidenceRecord).
- Consensus: Leader-based (Authority) with Ed25519 signatures.
- Networking: Basic HTTP replication.

The purpose of the project is to:
- Explore architectural patterns
- Validate technical decisions
- Experiment with blockchain-related concepts

---

## Technical Stack (Fixed)

- Language: **Java 25**
- Framework: **Spring Boot 4**
- Architecture: **Hexagonal Architecture (Ports & Adapters) + DDD**
- Build: **Maven** (Wrapper `./mvnw` mandatory)
- Containerization: **Docker** & **Docker Compose**
- CI: **GitHub Actions**

Agents MUST NOT suggest alternative stacks unless explicitly asked.

---

## Architecture Rules (Strict)

### Layering & Packages

The backend MUST follow strict separation with this specific package structure:

- `domain`: Framework-agnostic models (`Block`, `EvidenceRecord`).
- `application`:
    - `service`: Use cases implementation (`LedgerService`).
    - `port.out`: Interfaces for infrastructure dependencies (`CryptoPort`, `ReplicationPort`).
- `infrastructure`:
    - `adapter`: Implementations of output ports (`CryptoAdapter`, `ReplicationAdapter`).
    - `config`: Spring configuration classes.
- `interfaces`:
    - `rest`: Controllers, DTOs, and Exception Handlers.

### Configuration Pattern

- **Configuration classes** (e.g., `@ConfigurationProperties`) MUST reside in `infrastructure/config`.
- They MUST implement an **Output Port** defined in `application.port.out`.
- The Application layer MUST depend only on the Port, never on the concrete Configuration class.

### Domain-Driven Design (DDD)

- Aggregates must be explicit where relevant.
- Invariants must be enforced in domain objects and/or use cases.
- Entities and Value Objects must be clearly separated.
- Value Objects must be immutable (Java `record` preferred).

---

## Security Standards (Critical)

### General & Secrets
- **Zero Trust in Code**: Never hardcode private keys, passwords, or secrets.
- **Environment Variables**: Use `${VAR_NAME}` placeholders.
- **Local Development**: Use a `.env` file (strictly ignored by git).

### Application Security (OWASP)
- **Input Validation**: All external inputs (REST, CLI) must be strictly validated using Bean Validation (`@Valid`, `@NotBlank`) before processing.
- **Error Handling**: Never leak stack traces or internal details to the client. Use `GlobalExceptionHandler`.
- **Injection Prevention**: Although no DB is currently used, any future persistence must use parameterized queries.
- **Dependencies**: Keep dependencies updated to avoid known vulnerabilities (CVEs).

### Blockchain Security
- **Cryptographic Integrity**:
    - Signing: **Ed25519** (Standard).
    - Hashing: **SHA-256** (Standard).
    - Custom cryptography is **strictly forbidden**.
- **Signature Verification**: Must occur **before** any business logic or state change.
- **Determinism**: Block hash calculation must be deterministic (Canonicalization of fields is mandatory).
- **Immutability**: The ledger must be append-only. History rewriting is forbidden in the domain logic.

---

## Development Methodology: TDD & BDD (Mandatory)

### TDD (Test-Driven Development)

For changes that affect behavior (new features, bug fixes), the agent SHOULD default to TDD:

1. **Red**: Write a failing test that captures the expected behavior.
2. **Green**: Implement the minimal code to make the test pass.
3. **Refactor**: Improve structure without changing behavior.

Rules:
- **Unit Tests**: JUnit 5 + Mockito.
- **Scope**: Domain logic, Application Services, and Adapters.
- **Isolation**: Unit tests for Application Services should Mock the Ports.

---

## Coding Standards & Style

The project enforces strict coding standards via **Maven Checkstyle Plugin**.

- **Style Guide**: **Google Java Style**.
- **Configuration**: The agent MUST respect the style defined in `style/intellij-java-google-style.xml`.
- **Indentation**: 2 spaces (standard Google Style).
- **Imports**: No wildcard imports (`import java.util.*` is forbidden).
- **Naming**:
    - Classes: `PascalCase`
    - Methods/Variables: `camelCase`
    - Constants: `UPPER_SNAKE_CASE`
- **JavaDoc**: Mandatory for all public classes and interfaces.

The agent MUST ensure that any generated code complies with these rules to avoid breaking the CI build.

---

## Documentation & Language

- **Language**: All code, comments, JavaDoc, and commit messages MUST be in **English**.
- **JavaDoc**:
    - Mandatory for **all Interfaces (Ports)**.
    - Mandatory for **Public Methods** in Services and Adapters.
    - Must explain the "What" and "Why", not just the "How".
- **README**: Must be kept up-to-date with new endpoints, configuration steps, or key generation guides.

---

## Output Expectations

Every agent response that proposes or provides changes MUST include:

- What changed
- Why it changed
- Where it fits in the hexagonal architecture
- Test impact (added/updated tests)

When modifying existing code:
- Respect existing structure.
- Avoid scope creep.
- Do not perform unrelated refactors.

---

## Enforcement

If the agent cannot comply with any rule:

1. Explicitly state the conflict.
2. Explain why compliance is not possible.
3. Provide a compliant alternative.

Silent deviation is considered a failure.

---

**AGENTS.md is binding.**
All backend-related AI interactions must comply with it.
