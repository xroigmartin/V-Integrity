# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

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

### Fixed
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
- **Security**: Externalized private/public keys using environment variables (`.env` support).
- **Testing**: Added unit tests for core services and adapters.
- **Documentation**: Comprehensive JavaDoc for all classes and ports.

### Changed
- Refactored project structure to strictly follow Hexagonal Architecture & DDD rules defined in `AGENTS.md`.
- Replaced Python-based `.gitignore` with Java/IntelliJ specific configuration.
