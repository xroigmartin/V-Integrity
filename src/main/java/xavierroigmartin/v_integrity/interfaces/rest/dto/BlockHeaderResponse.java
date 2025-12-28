package xavierroigmartin.v_integrity.interfaces.rest.dto;

import java.time.Instant;

/**
 * Lightweight representation of a block header.
 * Used for quick comparison of chain state between nodes.
 *
 * @param height         Block height.
 * @param hash           Block hash.
 * @param proposerNodeId ID of the node that proposed the block.
 * @param timestamp      Block creation timestamp.
 */
public record BlockHeaderResponse(
    long height,
    String hash,
    String proposerNodeId,
    Instant timestamp
) {}
