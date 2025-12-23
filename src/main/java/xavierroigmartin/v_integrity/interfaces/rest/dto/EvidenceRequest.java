package xavierroigmartin.v_integrity.interfaces.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record EvidenceRequest(
        @NotBlank String homologationId,
        @NotBlank String testRunId,

        @NotBlank String artifactName,
        @NotBlank String artifactType,

        @NotBlank String hashAlgorithm,
        @NotBlank String hash,
        Long sizeBytes,

        @NotBlank String createdBy,
        String storageUri,

        List<String> standards
) {}
