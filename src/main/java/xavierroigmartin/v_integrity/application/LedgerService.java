package xavierroigmartin.v_integrity.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import xavierroigmartin.v_integrity.application.port.out.CryptoPort;
import xavierroigmartin.v_integrity.application.port.out.HashingPort;
import xavierroigmartin.v_integrity.application.port.out.LogPort;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;
import xavierroigmartin.v_integrity.application.port.out.ReplicationPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;

/**
 * Core application service that manages the blockchain ledger state.
 * <p>
 * This service is responsible for:
 * <ul>
 *     <li>Maintaining the in-memory blockchain (PoC).</li>
 *     <li>Managing the mempool of pending evidences.</li>
 *     <li>Creating new blocks (mining/committing) if the node is a leader.</li>
 *     <li>Validating and accepting blocks replicated from other nodes.</li>
 * </ul>
 */
@Service
public class LedgerService {

    private final NodeConfigurationPort nodeConfig;
    private final HashingPort hashing;
    private final CryptoPort crypto;
    private final ReplicationPort replication;
    private final LogPort logger;

    // In-memory state (PoC)
    private final List<Block> chain = new ArrayList<>();
    private final List<EvidenceRecord> mempool = new ArrayList<>();
    private final AtomicLong evidenceSequence = new AtomicLong(0);

    public LedgerService(NodeConfigurationPort nodeConfig, HashingPort hashing, CryptoPort crypto, ReplicationPort replication, LogPort logger) {
        this.nodeConfig = nodeConfig;
        this.hashing = hashing;
        this.crypto = crypto;
        this.replication = replication;
        this.logger = logger;
        chain.add(createGenesis());
    }

    /**
     * Returns a read-only copy of the current blockchain.
     *
     * @return List of blocks in the chain.
     */
    public synchronized List<Block> chain() {
        return List.copyOf(chain);
    }

    /**
     * Returns a read-only copy of the current mempool (pending evidences).
     *
     * @return List of pending evidences.
     */
    public synchronized List<EvidenceRecord> mempool() {
        return List.copyOf(mempool);
    }

    /**
     * Submits a new evidence record to the mempool.
     * <p>
     * Performs basic validation and normalization before adding it to the pending list.
     *
     * @param evidence The evidence record to submit.
     * @return The normalized evidence record as stored in the mempool.
     * @throws IllegalArgumentException if the hash algorithm is not supported or the hash format is invalid.
     */
    public EvidenceRecord submitEvidence(EvidenceRecord evidence) {
        // Minimal PoC validations
        String algo = normalizeAlgo(evidence.hashAlgorithm());
        if (!"SHA-256".equals(algo)) {
            throw new IllegalArgumentException("Supported hashAlgorithm in PoC: SHA-256");
        }
        if (!isValidHexSha256(evidence.hash())) {
            throw new IllegalArgumentException("Invalid hash: must be a 64-character hex string (SHA-256)");
        }

        EvidenceRecord normalized = new EvidenceRecord(
                evidence.evidenceId(),
                evidence.homologationId(),
                evidence.testRunId(),
                evidence.artifactName(),
                evidence.artifactType(),
                "SHA-256",
                evidence.hash().toLowerCase(Locale.ROOT),
                evidence.sizeBytes(),
                evidence.createdBy(),
                evidence.storageUri(),
                evidence.standards(),
                evidence.createdAt()
        );

        synchronized (this) {
            mempool.add(normalized);
            evidenceSequence.incrementAndGet();
        }

        logger.logBusinessEvent("EVIDENCE_SUBMITTED", Map.of(
            "evidenceId", normalized.evidenceId(),
            "hash", normalized.hash(),
            "createdBy", normalized.createdBy()
        ));

        return normalized;
    }

    /**
     * Leader only: Seals pending evidences from the mempool into a new signed block and replicates it.
     *
     * @return The newly created and committed block.
     * @throws IllegalStateException if the node is not a leader, has no private key, or the mempool is empty.
     */
    public Block commitAsLeader() {
        if (!nodeConfig.isLeader()) {
            throw new IllegalStateException("This node is not a leader; cannot commit blocks.");
        }
        if (nodeConfig.getPrivateKeyBase64() == null || nodeConfig.getPrivateKeyBase64().isBlank()) {
            throw new IllegalStateException("Missing ledger.node.privateKeyBase64 to sign blocks.");
        }

        final Block newBlock;
        final List<String> peerUrls = nodeConfig.getPeers();

        synchronized (this) {
            if (mempool.isEmpty()) {
                throw new IllegalStateException("No pending evidences in mempool.");
            }

            Block prev = latest();
            long height = prev.height() + 1;
            Instant ts = Instant.now();
            List<EvidenceRecord> evidences = List.copyOf(mempool);
            String previousHash = prev.hash();
            String proposer = nodeConfig.getNodeId();

            String canonical = canonicalBlockFields(height, ts, evidences, previousHash, proposer);
            String hashHex = hashing.sha256Hex(canonical);
            byte[] hashBytes = hexToBytes(hashHex);

            String signature = crypto.signEd25519(hashBytes, nodeConfig.getPrivateKeyBase64());

            newBlock = new Block(height, ts, evidences, previousHash, proposer, hashHex, signature);

            // append-only
            chain.add(newBlock);
            mempool.clear();
        }

        logger.logBusinessEvent("BLOCK_COMMITTED", Map.of(
            "height", newBlock.height(),
            "hash", newBlock.hash(),
            "evidencesCount", newBlock.evidences().size(),
            "proposer", newBlock.proposerNodeId()
        ));

        replication.replicateBlockToPeers(newBlock, peerUrls);
        return newBlock;
    }

    /**
     * Followers: Receive a block already sealed by a leader, validate it, and accept it.
     *
     * @param incoming The block received from a peer.
     * @throws IllegalArgumentException if the block is invalid (height, hash, signature, etc.).
     */
    public synchronized void acceptReplicatedBlock(Block incoming) {
        Block prev = latest();

        if (incoming.height() != prev.height() + 1) {
            String msg = "Invalid Height. Expected " + (prev.height() + 1) + " but received " + incoming.height();
            logger.logBusinessError("INVALID_BLOCK_HEIGHT", msg, Map.of("proposer", incoming.proposerNodeId()));
            throw new IllegalArgumentException(msg);
        }
        if (!Objects.equals(incoming.previousHash(), prev.hash())) {
            throw new IllegalArgumentException("Invalid previousHash.");
        }

        String pubKey = nodeConfig.getAllowedNodePublicKeys().get(incoming.proposerNodeId());
        if (pubKey == null || pubKey.isBlank()) {
            throw new IllegalArgumentException("Unauthorized Proposer: " + incoming.proposerNodeId());
        }

        // Recompute hash
        String canonical = canonicalBlockFields(
                incoming.height(),
                incoming.timestamp(),
                incoming.evidences(),
                incoming.previousHash(),
                incoming.proposerNodeId()
        );
        String recomputedHash = hashing.sha256Hex(canonical);

        if (!Objects.equals(recomputedHash, incoming.hash())) {
            logger.logBusinessError("INVALID_BLOCK_HASH", "Hash mismatch", Map.of("received", incoming.hash(), "computed", recomputedHash));
            throw new IllegalArgumentException("Invalid Hash (does not match recomputed hash).");
        }

        // Verify signature
        byte[] hashBytes = hexToBytes(incoming.hash());
        boolean okSig = crypto.verifyEd25519(hashBytes, incoming.signature(), pubKey);
        if (!okSig) {
            logger.logBusinessError("INVALID_BLOCK_SIGNATURE", "Signature verification failed", Map.of("proposer", incoming.proposerNodeId()));
            throw new IllegalArgumentException("Invalid signature for proposer " + incoming.proposerNodeId());
        }

        // append-only
        chain.add(incoming);

        // PoC: remove confirmed evidences from mempool if they exist
        mempool.removeAll(incoming.evidences());

        logger.logBusinessEvent("BLOCK_ACCEPTED", Map.of(
            "height", incoming.height(),
            "hash", incoming.hash(),
            "proposer", incoming.proposerNodeId()
        ));
    }

    /**
     * Validates the integrity of the entire local blockchain.
     *
     * @return true if the chain is valid, false otherwise.
     */
    public synchronized boolean isValidLocalChain() {
        if (chain.isEmpty()) return false;

        for (int i = 1; i < chain.size(); i++) {
            Block prev = chain.get(i - 1);
            Block cur = chain.get(i);

            if (cur.height() != prev.height() + 1) return false;
            if (!Objects.equals(cur.previousHash(), prev.hash())) return false;

            String pubKey = nodeConfig.getAllowedNodePublicKeys().get(cur.proposerNodeId());
            if (pubKey == null || pubKey.isBlank()) return false;

            String canonical = canonicalBlockFields(cur.height(), cur.timestamp(), cur.evidences(), cur.previousHash(), cur.proposerNodeId());
            String recomputedHash = hashing.sha256Hex(canonical);
            if (!Objects.equals(recomputedHash, cur.hash())) return false;

            byte[] hashBytes = hexToBytes(cur.hash());
            if (!crypto.verifyEd25519(hashBytes, cur.signature(), pubKey)) return false;
        }

        return true;
    }

    /**
     * Searches for an evidence by its hash in the entire chain.
     *
     * @param hashHex The SHA-256 hash of the evidence.
     * @return An Optional containing the EvidenceProof (evidence + block) if found.
     */
    public synchronized Optional<EvidenceProof> findEvidenceByHash(String hashHex) {
        String h = hashHex == null ? "" : hashHex.trim().toLowerCase(Locale.ROOT);

        for (Block b : chain) {
            for (EvidenceRecord e : b.evidences()) {
                if (e.hash().equals(h)) {
                    return Optional.of(new EvidenceProof(e, b));
                }
            }
        }
        return Optional.empty();
    }

    private Block createGenesis() {
        long height = 0;
        Instant ts = Instant.parse("2020-01-01T00:00:00Z");
        List<EvidenceRecord> evidences = List.of();
        String previousHash = "0".repeat(64);
        String proposer = "GENESIS";

        String canonical = canonicalBlockFields(height, ts, evidences, previousHash, proposer);
        String hash = hashing.sha256Hex(canonical);

        return new Block(height, ts, evidences, previousHash, proposer, hash, "GENESIS");
    }

    private Block latest() {
        return chain.get(chain.size() - 1);
    }

    private String canonicalBlockFields(long height, Instant ts, List<EvidenceRecord> evidences, String previousHash, String proposer) {
        // Deterministic canonicalization (very important)
        StringBuilder sb = new StringBuilder();
        sb.append("height=").append(height).append("|");
        sb.append("ts=").append(ts.toString()).append("|");
        sb.append("prev=").append(previousHash).append("|");
        sb.append("proposer=").append(proposer).append("|");
        sb.append("evidences=");

        // Deterministic sort by evidenceId
        List<EvidenceRecord> sorted = new ArrayList<>(evidences);
        sorted.sort(Comparator.comparing(EvidenceRecord::evidenceId));

        for (EvidenceRecord e : sorted) {
            sb.append(e.evidenceId()).append(",");
            sb.append(e.homologationId()).append(",");
            sb.append(e.testRunId()).append(",");
            sb.append(e.artifactName()).append(",");
            sb.append(e.artifactType()).append(",");
            sb.append(e.hashAlgorithm()).append(",");
            sb.append(e.hash()).append(",");
            sb.append(e.sizeBytes() == null ? "" : e.sizeBytes()).append(",");
            sb.append(e.createdBy()).append(",");
            sb.append(e.storageUri() == null ? "" : e.storageUri()).append(",");
            sb.append(e.createdAt().toString()).append(",");

            // sorted standards
            List<String> std = e.standards() == null ? List.of() : e.standards();
            List<String> stdSorted = new ArrayList<>(std);
            stdSorted.sort(String::compareTo);
            sb.append(String.join("+", stdSorted));

            sb.append(";");
        }

        return sb.toString();
    }

    private static boolean isValidHexSha256(String hex) {
        if (hex == null) return false;
        String h = hex.trim();
        if (h.length() != 64) return false;
        for (int i = 0; i < h.length(); i++) {
            char c = h.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!ok) return false;
        }
        return true;
    }

    private static String normalizeAlgo(String algo) {
        return algo == null ? "" : algo.trim().toUpperCase(Locale.ROOT);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }

    public record EvidenceProof(EvidenceRecord evidence, Block block) {}
}
