package xavierroigmartin.v_integrity.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record EvidenceRecord(
        @NotBlank String evidenceId,      // UUID string
        @NotBlank String homologationId,
        @NotBlank String testRunId,

        @NotBlank String artifactName,
        @NotBlank String artifactType,

        @NotBlank String hashAlgorithm,   // "SHA-256"
        @NotBlank String hash,            // hex
        Long sizeBytes,                   // opcional PoC

        @NotBlank String createdBy,
        String storageUri,                // opcional PoC

        List<String> standards,           // opcional PoC
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

