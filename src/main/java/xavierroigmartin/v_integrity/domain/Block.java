package xavierroigmartin.v_integrity.domain;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing a block in the blockchain.
 * <p>
 * A block contains a list of evidences, links to the previous block, and is cryptographically signed.
 *
 * @param height         The index of the block in the chain (0 for Genesis).
 * @param timestamp      When the block was created.
 * @param evidences      List of evidences included in this block.
 * @param previousHash   SHA-256 hash of the previous block.
 * @param proposerNodeId ID of the node that proposed (mined) this block.
 * @param hash           SHA-256 hash of this block's canonical content.
 * @param signature      Ed25519 signature of the block hash by the proposer.
 */
public record Block(
        long height,
        Instant timestamp,
        List<EvidenceRecord> evidences,
        String previousHash,

        String proposerNodeId,
        String hash,
        String signature
) {}
