# Changelog

All notable changes to this project will be documented in this file.

## [0.2.0] - 2025-05-21

### Added
- **Block Synchronization (Catch-up)**: Implemented a pull-based mechanism (`SyncService`) to allow follower nodes to recover missing blocks after downtime.
- **Sync Endpoints**:
  - `GET /api/blocks/latest`: Returns lightweight block header for state comparison.
  - `GET /api/blocks`: Returns a range of blocks (pagination support).
  - `POST /api/sync`: Manually triggers the synchronization process.
- **Idempotency**: Enhanced `LedgerService` to safely handle duplicate blocks during sync.
- **Docker Network**: Updated `docker-compose.yml` to include a 3rd node and fixed internal networking aliases.

### Fixed
- **Docker Permissions**: Updated `Dockerfile` to create and assign permissions to the `logs` directory for the non-root user.
- **Peer Configuration**: Correctly configured peer URLs in `application-node2.yml` and `application-node3.yml`.

## [0.1.1] - 2025-05-21

### Added
- **API Documentation**: Integrated Swagger/OpenAPI (SpringDoc) with detailed endpoint descriptions.
- **Docker Support**: Added `Dockerfile` (multi-stage build) and `docker-compose.yml` for easy deployment.
- **CI/CD**: Added GitHub Actions workflow for automated testing and building.
- **Maven Wrapper**: Included Maven Wrapper for reproducible builds.

## [0.1.0] - 2025-05-21

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
