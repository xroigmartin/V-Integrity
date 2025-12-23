package xavierroigmartin.v_integrity.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * Domain model representing a piece of evidence to be secured in the blockchain.
 * <p>
 * This record captures metadata about an artifact (file, log, report) generated during a process (e.g., testing).
 *
 * @param evidenceId     Unique identifier for this evidence (UUID).
 * @param homologationId Identifier of the homologation process.
 * @param testRunId      Identifier of the specific test run.
 * @param artifactName   Name of the artifact (e.g., "execution.log").
 * @param artifactType   Type of the artifact (e.g., "LOG", "PDF").
 * @param hashAlgorithm  Algorithm used for the hash (e.g., "SHA-256").
 * @param hash           Hexadecimal representation of the artifact's hash.
 * @param sizeBytes      Size of the artifact in bytes (optional).
 * @param createdBy      User or system that created the evidence.
 * @param storageUri     URI where the actual artifact is stored (optional).
 * @param standards      List of standards this evidence complies with (optional).
 * @param createdAt      Timestamp when the evidence was created.
 */
public record EvidenceRecord(
        @NotBlank String evidenceId,
        @NotBlank String homologationId,
        @NotBlank String testRunId,

        @NotBlank String artifactName,
        @NotBlank String artifactType,

        @NotBlank String hashAlgorithm,
        @NotBlank String hash,
        Long sizeBytes,

        @NotBlank String createdBy,
        String storageUri,

        List<String> standards,
        Instant createdAt
) {
    public EvidenceRecord {
        if (evidenceId == null || evidenceId.isBlank()) {
            evidenceId = UUID.randomUUID().toString();
        }
        if (createdAt == null) createdAt = Instant.now();
        if (standards == null) standards = List.of();
    }
}
