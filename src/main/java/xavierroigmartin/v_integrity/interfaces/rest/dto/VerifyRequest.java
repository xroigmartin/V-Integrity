package xavierroigmartin.v_integrity.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(
        @NotBlank String hash
) {}