# V-Integrity

![Version](https://img.shields.io/badge/version-0.2.0-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

[See Changelog](CHANGELOG.md) | [Contributing Guide](CONTRIBUTING.md)

**V-Integrity** is a backend Proof of Concept (PoC) for a blockchain-based evidence integrity system. It allows securing digital evidences (logs, reports, artifacts) in an immutable ledger, ensuring traceability and non-repudiation.

## 🚀 Features

- **Immutable Ledger**: Stores evidences in cryptographically linked blocks (SHA-256).
- **Digital Signatures**: Uses **Ed25519** for signing blocks and verifying proposer identity.
- **Hexagonal Architecture**: Strictly follows Ports & Adapters pattern to decouple domain logic from infrastructure.
- **REST API**: Provides endpoints to submit evidences, query the chain, and verify integrity.
- **Replication & Sync**:
    - **Push**: Real-time block propagation to peers.
    - **Pull (Catch-up)**: Synchronization mechanism for nodes recovering from downtime.
    - **Auto-Sync**: Nodes automatically sync with peers on startup.
- **Docker Ready**: Includes Dockerfile and Compose for instant deployment.
- **OpenAPI Docs**: Interactive API documentation via Swagger UI.

## 🛠️ Tech Stack

- **Language**: Java 25
- **Framework**: Spring Boot 4
- **Architecture**: Hexagonal (Ports & Adapters) + DDD
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose

## 🏗️ Architecture

The project follows a strict Hexagonal Architecture:

- **Domain**: Core business logic and models (`Block`, `EvidenceRecord`). Framework-agnostic.
- **Application**: Use cases and Ports (`LedgerService`, `SyncService`, `CryptoPort`).
- **Infrastructure**: Adapters for external concerns (`CryptoAdapter`, `ReplicationAdapter`, `SyncAdapter`, `AutoSyncAdapter`).
- **Interfaces**: Entry points to the application (`LedgerController`).

## 🐳 Running with Docker (Recommended)

You can spin up a 3-node network (1 Leader + 2 Followers) instantly:

1.  Ensure you have a `.env` file with your keys (see Configuration).
2.  Run:
    ```bash
    docker-compose up --build
    ```
3.  Access nodes:
    - **Node 1 (Leader)**: `http://localhost:8081`
    - **Node 2 (Follower)**: `http://localhost:8082`
    - **Node 3 (Follower)**: `http://localhost:8083`

## ⚙️ Configuration

### Generating Keys (Ed25519)

Since this project uses Ed25519 signatures, you need to generate a key pair. You can use OpenSSL:

1. **Generate Private Key:**
   ```bash
   openssl genpkey -algorithm ED25519 -out private.pem
   ```

2. **Generate Public Key:**
   ```bash
   openssl pkey -in private.pem -pubout -out public.pem
   ```

3. **Convert to Base64 (for configuration):**
   - **Private Key (PKCS#8):**
     ```bash
     openssl pkcs8 -topk8 -inform PEM -outform DER -in private.pem -nocrypt | base64 -w 0
     ```
   - **Public Key (X.509):**
     ```bash
     openssl pkey -pubin -inform PEM -outform DER -in public.pem | base64 -w 0
     ```

Copy the Base64 outputs into your `.env` file or environment variables.

### Environment Variables

Security is paramount. Private keys are **never** hardcoded. You must configure the following environment variables (e.g., via a `.env` file or IDE run configuration):

```properties
# Private Key for signing blocks (Base64 PKCS#8)
LEDGER_PRIVATE_KEY_BASE64=...

# Public Key of the authorized node (Base64 X.509)
LEDGER_NODE1_PUBLIC_KEY_BASE64=...
```

### Application Properties

Configure node identity and peers in `application.yaml` (default node) or specific profile files like `application-node2.yml`.

**Example `application.yaml` (Leader Node):**
```yaml
server:
  port: 8081
ledger:
  node:
    nodeId: "node-1"
    leader: true
    peers:
      - "http://localhost:8082"
      - "http://localhost:8083"
```

**Example `application-node2.yml` (Follower Node):**
```yaml
server:
  port: 8082
ledger:
  node:
    nodeId: "node-2"
    leader: false
    peers:
      - "http://localhost:8081"
```

To run a specific node profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=node2
```

## 🔌 API Endpoints

Full API documentation is available via Swagger UI when running the application:
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8081/v3/api-docs`

### 1. Submit Evidence
Registers a new evidence in the node's mempool (pending to be mined).

*   **URL**: `POST /api/evidences`
*   **Body**:
    ```json
    {
      "homologationId": "HOM-123",
      "testRunId": "RUN-456",
      "artifactName": "test.log",
      "artifactType": "LOG",
      "hashAlgorithm": "SHA-256",
      "hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "createdBy": "system-user",
      "storageUri": "s3://bucket/path/to/file",
      "standards": ["ISO-27001"]
    }
    ```
*   **Response**: `201 Created` with the stored evidence details.

### 2. Commit Block (Leader Only)
Triggers the creation of a new block containing all pending evidences in the mempool. The block is signed and replicated to peers.

*   **URL**: `POST /api/blocks/commit`
*   **Body**: Empty
*   **Response**: `201 Created` with the new block details.

### 3. Verify Evidence
Verifies if a specific hash exists in the blockchain and returns a cryptographic proof.

*   **URL**: `POST /api/verify`
*   **Body**:
    ```json
    {
      "hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
    ```
*   **Response**:
    ```json
    {
      "verified": true,
      "hash": "...",
      "evidence": { ... },
      "proof": {
        "blockHeight": 1,
        "blockHash": "...",
        "signatureValid": true
      }
    }
    ```

### 4. Synchronize (Catch-up)
Manually triggers the synchronization process to fetch missing blocks from a peer.

*   **URL**: `POST /api/sync`
*   **Body** (Optional):
    ```json
    {
      "sourcePeerUrl": "http://node-1:8081"
    }
    ```
*   **Response**:
    ```json
    {
      "synced": true,
      "appliedBlocks": 5,
      "fromHeight": 10,
      "toHeight": 15
    }
    ```

### 5. Get Chain
Retrieves the full local blockchain.

*   **URL**: `GET /api/chain`
*   **Response**: JSON containing the list of all blocks.

### 6. Validate Chain
Checks the cryptographic integrity of the local chain (hashes and links).

*   **URL**: `GET /api/validate`
*   **Response**: `{"valid": true}`

## 🧪 Testing

Run unit tests with Maven:

```bash
./mvnw test
```

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
