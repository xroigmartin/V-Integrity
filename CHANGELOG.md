# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
- **Security**: Implemented Role-Based Access Control (RBAC) in PostgreSQL with `ledger_owner` (DDL) and `ledger_app` (DML) roles.
- **Database Migration**: Decoupled Flyway migration from application startup. Added a dedicated `flyway-migrator` service in Docker Compose.
- **Database Schema**: Created initial Flyway migration `V1__init_ledger.sql` defining `blocks`, `evidences`, and `block_evidences` tables in a dedicated `ledger` schema.
- **Immutability**: Implemented PostgreSQL triggers to enforce append-only behavior (blocking UPDATE/DELETE) on ledger tables.
- **Persistence Infrastructure**: Added PostgreSQL 17 to `docker-compose.yml` and configured Spring Boot Data JPA with Flyway support.
- **Database Configuration**: Updated `application.yaml` and node-specific profiles to connect to the local PostgreSQL instance.
- **Dependencies**: Added `spring-boot-starter-data-jpa`, `postgresql`, and `flyway-core` to `pom.xml`.

## [0.3.0] - 2026-01-06

### Added
- **Continuous Documentation Rules**: Updated `AGENTS.md` to mandate automatic updates of `CHANGELOG.md`, `README.md`, and `PR.md`.
- **Mandatory Integration Tests**: Defined rules for using `@SpringBootTest` and `WireMock` for external communication tests.
- **OpenAPI Enforcement**: Mandatory Swagger annotations for all REST endpoints.
- **Workflow**: 
  - Added `.github/PULL_REQUEST_TEMPLATE.md` and configured `.gitignore` for local `PR.md` drafts.
  - Optimized GitHub Actions workflow to separate unit and integration tests, and run full verification only on Pull Requests.
  - Added support for prefixed release branches (e.g., `chore/prepare-v*`) in CI triggers.
- **Integration Tests**: 
  - Added `LedgerControllerIntegrationTest` covering all public endpoints and error scenarios.
  - Configured secure test execution using environment variables for cryptographic keys.
- **Test Infrastructure**: Separated Unit and Integration tests execution via Maven plugins (`surefire` vs `failsafe`).
- **Agent Rules**: Updated `AGENTS.md` with stricter TDD enforcement, explicit error handling standards (RFC 7807), context-efficient documentation rules, and mandatory HTTP file tests.
- **Error Codes**: Introduced standard application error codes (e.g., `ERR_VALIDATION`, `ERR_BLOCK_INVALID`) in API responses.
- **HTTP Tests**: Added domain-specific HTTP test files (`api-evidences.http`, `api-blocks.http`, `api-node-ops.http`) covering all endpoints.

### Fixed
- **API Error Handling**: 
  - Refactored error handling to use specific Domain and Application exceptions (`InvalidBlockException`, `NodeNotLeaderException`, etc.).
  - Updated `GlobalExceptionHandler` to map these exceptions to HTTP 400 (Bad Request) instead of 500.
  - Updated `LedgerService` and `CryptoAdapter` to throw the new specific exceptions.
  - **RFC 7807 Problem Details**: Migrated API error responses to standard `ProblemDetail` format (replacing custom `ErrorResponse`).
  - **Validation Errors**: Unified `@Valid` validation errors to return the standard `ProblemDetail` format.
  - **Error Codes**: Added `errorCode` field to `ProblemDetail` responses for programmatic error handling.
- **Swagger Documentation**: Updated OpenAPI annotations in `LedgerController` to reflect correct error codes (400).
- **Dependencies**: Added `spring-boot-starter-test`, `spring-boot-resttestclient`, and `spring-boot-restclient` to `pom.xml` to resolve missing `TestRestTemplate`.

## [0.2.0] - 2025-12-28

### Added
- **Block Synchronization (Catch-up)**: Implemented a pull-based mechanism (`SyncService`) to allow follower nodes to recover missing blocks after downtime.
- **Automatic Synchronization**: Implemented `AutoSyncAdapter` to trigger catch-up on application startup.
- **Sync Endpoints**:
  - `GET /api/blocks/latest`: Returns lightweight block header for state comparison.
  - `GET /api/blocks`: Returns a range of blocks (pagination support).
  - `POST /api/sync`: Manually triggers the synchronization process.
- **Idempotency**: Enhanced `LedgerService` to safely handle duplicate blocks during sync.
- **Docker Network**: Updated `docker-compose.yml` to include a 3rd node, fixed internal networking aliases, and added healthchecks.

### Fixed
- **Docker Permissions**: Updated `Dockerfile` to create and assign permissions to the `logs` directory for the non-root user.
- **Peer Configuration**: Correctly configured peer URLs in `application-node2.yml` and `application-node3.yml`.
- **Startup Optimization**: Leader node now skips initial sync to avoid connection errors with followers.

## [0.1.1] - 2025-12-24

### Added
- **API Documentation**: Integrated Swagger/OpenAPI (SpringDoc) with detailed endpoint descriptions.
- **Docker Support**: Added `Dockerfile` (multi-stage build) and `docker-compose.yml` for easy deployment.
- **CI/CD**: Added GitHub Actions workflow for automated testing and building.
- **Maven Wrapper**: Included Maven Wrapper for reproducible builds.

## [0.1.0] - 2025-12-23

### Added
- **Core Blockchain Logic**: Implemented `LedgerService` for managing blocks, evidences, and mempool.
- **Domain Models**: Defined `Block` and `EvidenceRecord` (replacing generic transactions) for evidence traceability.
- **Hexagonal Architecture**: Restructured project into `domain`, `application` (ports), `infrastructure` (adapters), and `interfaces`.
- **Cryptography**: Implemented Ed25519 signing/verification and SHA-256 hashing via `CryptoAdapter` and `HashingAdapter`.
- **Consensus (PoC)**: Basic leader-based block commitment and replication mechanism.
- **REST API**:
  - `POST /api/evidences`: Submit new evidence.
  - `POST /api/blocks/commit`: Trigger block creation (Leader only).
  - `GET /api/chain`: Retrieve the blockchain.
  - `POST /api/verify`: Verify evidence integrity and inclusion in the chain.
  - `Security`: Externalized private/public keys using environment variables (`.env` support).
- **Testing**: Added unit tests for core services and adapters.
- **Documentation**: Comprehensive JavaDoc for all classes and ports.

### Changed
- Refactored project structure to strictly follow Hexagonal Architecture & DDD rules defined in `AGENTS.md`.
- Replaced Python-based `.gitignore` with Java/IntelliJ specific configuration.
