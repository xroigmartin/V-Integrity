package xavierroigmartin.v_integrity.domain;

import java.time.Instant;
import java.util.List;

public record Block(
        long height,
        Instant timestamp,
        List<EvidenceRecord> evidences,
        String previousHash,

        String proposerNodeId,
        String hash,
        String signature
) {}