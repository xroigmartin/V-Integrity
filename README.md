# V-Integrity

![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

[See Changelog](CHANGELOG.md)

**V-Integrity** is a backend Proof of Concept (PoC) for a blockchain-based evidence integrity system. It allows securing digital evidences (logs, reports, artifacts) in an immutable ledger, ensuring traceability and non-repudiation.

## 🚀 Features

- **Immutable Ledger**: Stores evidences in cryptographically linked blocks (SHA-256).
- **Digital Signatures**: Uses **Ed25519** for signing blocks and verifying proposer identity.
- **Hexagonal Architecture**: Strictly follows Ports & Adapters pattern to decouple domain logic from infrastructure.
- **REST API**: Provides endpoints to submit evidences, query the chain, and verify integrity.
- **Replication**: Basic mechanism to propagate blocks to peer nodes.

## 🛠️ Tech Stack

- **Language**: Java 25
- **Framework**: Spring Boot 4
- **Architecture**: Hexagonal (Ports & Adapters) + DDD
- **Build Tool**: Maven

## 🏗️ Architecture

The project follows a strict Hexagonal Architecture:

- **Domain**: Core business logic and models (`Block`, `EvidenceRecord`). Framework-agnostic.
- **Application**: Use cases and Ports (`LedgerService`, `CryptoPort`, `NodeConfigurationPort`).
- **Infrastructure**: Adapters for external concerns (`CryptoAdapter`, `ReplicationAdapter`, `NodeProperties`).
- **Interfaces**: Entry points to the application (`LedgerController`).

## ⚙️ Configuration

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

### 4. Get Chain
Retrieves the full local blockchain.

*   **URL**: `GET /api/chain`
*   **Response**: JSON containing the list of all blocks.

### 5. Get Mempool
Retrieves the list of pending evidences waiting to be mined.

*   **URL**: `GET /api/mempool`

### 6. Validate Chain
Checks the cryptographic integrity of the local chain (hashes and links).

*   **URL**: `GET /api/validate`
*   **Response**: `{"valid": true}`

### 7. Get Evidence by Hash
Direct lookup of an evidence by its hash.

*   **URL**: `GET /api/evidences/hash/{hash}`

## 🧪 Testing

Run unit tests with Maven:

```bash
./mvnw test
```

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
