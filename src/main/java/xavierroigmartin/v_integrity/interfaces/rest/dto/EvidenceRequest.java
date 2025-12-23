package xavierroigmartin.v_integrity.interfaces.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for submitting new evidence via the REST API.
 * <p>
 * Contains all necessary metadata to register an artifact in the blockchain.
 *
 * @param homologationId Identifier of the homologation process.
 * @param testRunId      Identifier of the specific test run.
 * @param artifactName   Name of the artifact (e.g., "execution.log").
 * @param artifactType   Type of the artifact (e.g., "LOG", "PDF").
 * @param hashAlgorithm  Algorithm used for the hash (must be "SHA-256" in PoC).
 * @param hash           Hexadecimal representation of the artifact's hash.
 * @param sizeBytes      Size of the artifact in bytes (optional).
 * @param createdBy      User or system that created the evidence.
 * @param storageUri     URI where the actual artifact is stored (optional).
 * @param standards      List of standards this evidence complies with (optional).
 */
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
