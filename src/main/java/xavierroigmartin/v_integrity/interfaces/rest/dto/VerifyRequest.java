package xavierroigmartin.v_integrity.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for verifying an evidence by its hash.
 *
 * @param hash The SHA-256 hash of the evidence to verify.
 */
public record VerifyRequest(
        @NotBlank String hash
) {}
