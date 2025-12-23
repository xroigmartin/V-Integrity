package xavierroigmartin.v_integrity.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import xavierroigmartin.v_integrity.application.port.out.CryptoPort;
import xavierroigmartin.v_integrity.application.port.out.HashingPort;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;
import xavierroigmartin.v_integrity.application.port.out.ReplicationPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;

@Service
public class LedgerService {

    private final NodeConfigurationPort nodeConfig;
    private final HashingPort hashing;
    private final CryptoPort crypto;
    private final ReplicationPort replication;

    // Estado en memoria (PoC)
    private final List<Block> chain = new ArrayList<>();
    private final List<EvidenceRecord> mempool = new ArrayList<>();
    private final AtomicLong evidenceSequence = new AtomicLong(0);

    public LedgerService(NodeConfigurationPort nodeConfig, HashingPort hashing, CryptoPort crypto, ReplicationPort replication) {
        this.nodeConfig = nodeConfig;
        this.hashing = hashing;
        this.crypto = crypto;
        this.replication = replication;
        chain.add(createGenesis());
    }

    public synchronized List<Block> chain() {
        return List.copyOf(chain);
    }

    public synchronized List<EvidenceRecord> mempool() {
        return List.copyOf(mempool);
    }

    public EvidenceRecord submitEvidence(EvidenceRecord evidence) {
        // Validaciones PoC mínimas
        String algo = normalizeAlgo(evidence.hashAlgorithm());
        if (!"SHA-256".equals(algo)) {
            throw new IllegalArgumentException("hashAlgorithm soportado en PoC: SHA-256");
        }
        if (!isValidHexSha256(evidence.hash())) {
            throw new IllegalArgumentException("hash inválido: debe ser hex de 64 chars (SHA-256)");
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
        return normalized;
    }

    /**
     * Solo líder: sella evidencias del mempool en un bloque firmado y lo replica.
     */
    public Block commitAsLeader() {
        if (!nodeConfig.isLeader()) {
            throw new IllegalStateException("Este nodo no es líder; no puede hacer commit.");
        }
        if (nodeConfig.getPrivateKeyBase64() == null || nodeConfig.getPrivateKeyBase64().isBlank()) {
            throw new IllegalStateException("Falta ledger.node.privateKeyBase64 para firmar bloques.");
        }

        final Block newBlock;
        final List<String> peerUrls = nodeConfig.getPeers();

        synchronized (this) {
            if (mempool.isEmpty()) {
                throw new IllegalStateException("No hay evidencias pendientes en mempool.");
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

        replication.replicateBlockToPeers(newBlock, peerUrls);
        return newBlock;
    }

    /**
     * Followers: reciben bloque ya sellado por líder, validan y aceptan.
     */
    public synchronized void acceptReplicatedBlock(Block incoming) {
        Block prev = latest();

        if (incoming.height() != prev.height() + 1) {
            throw new IllegalArgumentException("Height inválido. Esperado " + (prev.height() + 1) + " y recibido " + incoming.height());
        }
        if (!Objects.equals(incoming.previousHash(), prev.hash())) {
            throw new IllegalArgumentException("previousHash inválido.");
        }

        String pubKey = nodeConfig.getAllowedNodePublicKeys().get(incoming.proposerNodeId());
        if (pubKey == null || pubKey.isBlank()) {
            throw new IllegalArgumentException("Proposer no autorizado: " + incoming.proposerNodeId());
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
            throw new IllegalArgumentException("Hash inválido (no coincide con recomputado).");
        }

        // Verify signature
        byte[] hashBytes = hexToBytes(incoming.hash());
        boolean okSig = crypto.verifyEd25519(hashBytes, incoming.signature(), pubKey);
        if (!okSig) {
            throw new IllegalArgumentException("Firma inválida para proposer " + incoming.proposerNodeId());
        }

        // append-only
        chain.add(incoming);

        // PoC: limpiamos mempool de evidencias ya confirmadas si existieran
        mempool.removeAll(incoming.evidences());
    }

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
        StringBuilder sb = new StringBuilder();
        sb.append("height=").append(height).append("|");
        sb.append("ts=").append(ts.toString()).append("|");
        sb.append("prev=").append(previousHash).append("|");
        sb.append("proposer=").append(proposer).append("|");
        sb.append("evidences=");

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
