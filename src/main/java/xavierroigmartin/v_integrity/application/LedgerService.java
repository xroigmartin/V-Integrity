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
import xavierroigmartin.v_integrity.application.exception.MempoolEmptyException;
import xavierroigmartin.v_integrity.application.exception.NodeNotLeaderException;
import xavierroigmartin.v_integrity.application.port.out.CryptoPort;
import xavierroigmartin.v_integrity.application.port.out.HashingPort;
import xavierroigmartin.v_integrity.application.port.out.LogPort;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;
import xavierroigmartin.v_integrity.application.port.out.PersistencePort;
import xavierroigmartin.v_integrity.application.port.out.ReplicationPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;
import xavierroigmartin.v_integrity.domain.exception.InvalidBlockException;
import xavierroigmartin.v_integrity.domain.exception.InvalidEvidenceException;
import xavierroigmartin.v_integrity.domain.exception.LedgerCorruptException;

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
  private final PersistencePort persistence;
  private final LogPort logger;

  // In-memory state (PoC)
  private final List<Block> chain = new ArrayList<>();
  private final List<EvidenceRecord> mempool = new ArrayList<>();
  private final AtomicLong evidenceSequence = new AtomicLong(0);

  public LedgerService(NodeConfigurationPort nodeConfig, HashingPort hashing, CryptoPort crypto,
      ReplicationPort replication, PersistencePort persistence, LogPort logger) {
    this.nodeConfig = nodeConfig;
    this.hashing = hashing;
    this.crypto = crypto;
    this.replication = replication;
    this.persistence = persistence;
    this.logger = logger;
    chain.add(createGenesis());
  }

  /**
   * Initializes the in-memory chain from a list of blocks (Rehydration).
   * Validates the integrity of the provided chain before accepting it.
   *
   * @param blocks The list of blocks loaded from storage.
   * @throws LedgerCorruptException if the chain is invalid or corrupt.
   */
  public synchronized void initializeChain(List<Block> blocks) {
    if (blocks == null || blocks.isEmpty()) {
      logger.logBusinessEvent("CHAIN_REHYDRATION_SKIPPED", Map.of("reason", "Empty source"));
      return;
    }

    logger.logBusinessEvent("CHAIN_REHYDRATION_STARTED", Map.of("blocksCount", blocks.size()));

    // Validate the incoming chain integrity
    for (int i = 0; i < blocks.size(); i++) {
      var current = blocks.get(i);
      
      // 1. Genesis Check
      if (i == 0) {
        if (current.height() != 0) {
           throw new LedgerCorruptException("First block must be height 0 (Genesis). Found: " + current.height());
        }
      } else {
        // 2. Link Check
        var prev = blocks.get(i - 1);
        if (current.height() != prev.height() + 1) {
          throw new LedgerCorruptException("Gap in chain. Expected height " + (prev.height() + 1) + " but found " + current.height());
        }
        if (!current.previousHash().equals(prev.hash())) {
          throw new LedgerCorruptException("Broken link at height " + current.height());
        }
        
        // 3. Signature & Hash Check
        validateBlockIntegrity(current);
      }
    }

    // If all good, replace in-memory chain
    this.chain.clear();
    this.chain.addAll(blocks);
    
    logger.logBusinessEvent("CHAIN_REHYDRATED", Map.of(
        "height", latest().height(),
        "blocks", chain.size()
    ));
  }

  private void validateBlockIntegrity(Block block) {
      // Skip signature check for Genesis (it's hardcoded/unsigned in this PoC or self-signed)
      if (block.height() == 0) return;

      var pubKey = nodeConfig.getAllowedNodePublicKeys().get(block.proposerNodeId());
      if (pubKey == null) {
          throw new LedgerCorruptException("Unknown proposer " + block.proposerNodeId() + " at height " + block.height());
      }

      // Recompute hash
      var canonical = canonicalBlockFields(
          block.height(),
          block.timestamp(),
          block.evidences(),
          block.previousHash(),
          block.proposerNodeId()
      );
      var recomputedHash = hashing.sha256Hex(canonical);
      
      if (!recomputedHash.equals(block.hash())) {
          throw new LedgerCorruptException("Invalid hash at height " + block.height());
      }

      // Verify signature
      var hashBytes = hexToBytes(block.hash());
      if (!crypto.verifyEd25519(hashBytes, block.signature(), pubKey)) {
          throw new LedgerCorruptException("Invalid signature at height " + block.height());
      }
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
   * Returns the latest block in the chain.
   *
   * @return The most recent block.
   */
  public synchronized Block latestBlock() {
    return latest();
  }

  /**
   * Retrieves a range of blocks starting from a specific height.
   *
   * @param fromHeightInclusive The starting height (inclusive).
   * @param limit               The maximum number of blocks to return.
   * @return A list of blocks.
   */
  public synchronized List<Block> getBlocksFromHeight(long fromHeightInclusive, int limit) {
    if (fromHeightInclusive < 0) {
      throw new IllegalArgumentException("fromHeight must be >= 0");
    }
    if (limit <= 0) {
      limit = 100;
    }

    int startIndex = (int) fromHeightInclusive;
    if (startIndex >= chain.size()) {
      return List.of();
    }

    int endIndex = Math.min(startIndex + limit, chain.size());
    return new ArrayList<>(chain.subList(startIndex, endIndex));
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
   * @throws InvalidEvidenceException if the hash algorithm is not supported or the hash format is
   *                                  invalid.
   */
  public EvidenceRecord submitEvidence(EvidenceRecord evidence) {
    // Minimal PoC validations
    var algo = normalizeAlgo(evidence.hashAlgorithm());
    if (!"SHA-256".equals(algo)) {
      throw new InvalidEvidenceException("Supported hashAlgorithm in PoC: SHA-256");
    }
    if (!isValidHexSha256(evidence.hash())) {
      throw new InvalidEvidenceException(
          "Invalid hash: must be a 64-character hex string (SHA-256)");
    }

    var normalized = new EvidenceRecord(
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
   * Leader only: Seals pending evidences from the mempool into a new signed block and replicates
   * it.
   *
   * @return The newly created and committed block.
   * @throws NodeNotLeaderException if the node is not a leader.
   * @throws MempoolEmptyException if the mempool is empty.
   * @throws IllegalStateException if the node has no private key.
   */
  public Block commitAsLeader() {
    if (!nodeConfig.isLeader()) {
      throw new NodeNotLeaderException("This node is not a leader; cannot commit blocks.");
    }
    if (nodeConfig.getPrivateKeyBase64() == null || nodeConfig.getPrivateKeyBase64().isBlank()) {
      throw new IllegalStateException("Missing ledger.node.privateKeyBase64 to sign blocks.");
    }

    final Block newBlock;
    final List<String> peerUrls = nodeConfig.getPeers();

    synchronized (this) {
      if (mempool.isEmpty()) {
        throw new MempoolEmptyException("No pending evidences in mempool.");
      }

      var prev = latest();
      long height = prev.height() + 1;
      var ts = Instant.now();
      var evidences = List.copyOf(mempool);
      var previousHash = prev.hash();
      var proposer = nodeConfig.getNodeId();

      var canonical = canonicalBlockFields(height, ts, evidences, previousHash, proposer);
      var hashHex = hashing.sha256Hex(canonical);
      var hashBytes = hexToBytes(hashHex);

      var signature = crypto.signEd25519(hashBytes, nodeConfig.getPrivateKeyBase64());

      newBlock = new Block(height, ts, evidences, previousHash, proposer, hashHex, signature);

      // append-only
      chain.add(newBlock);
      mempool.clear();
      
      // Persist immediately
      persistence.persistBlock(newBlock);
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
   * @throws InvalidBlockException if the block is invalid (height, hash, signature, etc.).
   */
  public synchronized void acceptReplicatedBlock(Block incoming) {
    var prev = latest();

    // Idempotency check: if we already have this block (same height, same hash), ignore it.
    if (incoming.height() <= prev.height()) {
      if (incoming.height() == prev.height() && incoming.hash().equals(prev.hash())) {
        logger.logBusinessEvent("BLOCK_IGNORED", Map.of(
            "reason", "Already exists",
            "height", incoming.height()
        ));
        // Ensure persistence is consistent even if in-memory had it
        persistence.persistBlock(incoming);
        return;
      }
      // If height is lower or same but different hash -> Fork or old block.
      // For PoC we just reject if it doesn't match next height.
      // But the user asked for idempotency.
      // If incoming.height <= prev.height(), we can check if chain.get(incoming.height) matches.
      var existing = chain.get((int) incoming.height());
      if (existing.hash().equals(incoming.hash())) {
         // Already have it
         persistence.persistBlock(incoming);
         return;
      } else {
         // Conflict/Fork
         throw new InvalidBlockException("Block height " + incoming.height() + " already exists with different hash.");
      }
    }

    if (incoming.height() != prev.height() + 1) {
      var msg =
          "Invalid Height. Expected " + (prev.height() + 1) + " but received " + incoming.height();
      logger.logBusinessError("INVALID_BLOCK_HEIGHT", msg,
          Map.of("proposer", incoming.proposerNodeId()));
      throw new InvalidBlockException(msg);
    }
    if (!Objects.equals(incoming.previousHash(), prev.hash())) {
      throw new InvalidBlockException("Invalid previousHash.");
    }

    var pubKey = nodeConfig.getAllowedNodePublicKeys().get(incoming.proposerNodeId());
    if (pubKey == null || pubKey.isBlank()) {
      throw new InvalidBlockException("Unauthorized Proposer: " + incoming.proposerNodeId());
    }

    // Recompute hash
    var canonical = canonicalBlockFields(
        incoming.height(),
        incoming.timestamp(),
        incoming.evidences(),
        incoming.previousHash(),
        incoming.proposerNodeId()
    );
    var recomputedHash = hashing.sha256Hex(canonical);

    if (!Objects.equals(recomputedHash, incoming.hash())) {
      logger.logBusinessError("INVALID_BLOCK_HASH", "Hash mismatch",
          Map.of("received", incoming.hash(), "computed", recomputedHash));
      throw new InvalidBlockException("Invalid Hash (does not match recomputed hash).");
    }

    // Verify signature
    var hashBytes = hexToBytes(incoming.hash());
    var okSig = crypto.verifyEd25519(hashBytes, incoming.signature(), pubKey);
    if (!okSig) {
      logger.logBusinessError("INVALID_BLOCK_SIGNATURE", "Signature verification failed",
          Map.of("proposer", incoming.proposerNodeId()));
      throw new InvalidBlockException(
          "Invalid signature for proposer " + incoming.proposerNodeId());
    }

    // append-only
    chain.add(incoming);

    // PoC: remove confirmed evidences from mempool if they exist
    mempool.removeAll(incoming.evidences());
    
    // Persist immediately
    persistence.persistBlock(incoming);

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
    if (chain.isEmpty()) {
      return false;
    }

    for (int i = 1; i < chain.size(); i++) {
      var prev = chain.get(i - 1);
      var cur = chain.get(i);

      if (cur.height() != prev.height() + 1) {
        return false;
      }
      if (!Objects.equals(cur.previousHash(), prev.hash())) {
        return false;
      }

      var pubKey = nodeConfig.getAllowedNodePublicKeys().get(cur.proposerNodeId());
      if (pubKey == null || pubKey.isBlank()) {
        return false;
      }

      var canonical = canonicalBlockFields(cur.height(), cur.timestamp(), cur.evidences(),
          cur.previousHash(), cur.proposerNodeId());
      var recomputedHash = hashing.sha256Hex(canonical);
      if (!Objects.equals(recomputedHash, cur.hash())) {
        return false;
      }

      var hashBytes = hexToBytes(cur.hash());
      if (!crypto.verifyEd25519(hashBytes, cur.signature(), pubKey)) {
        return false;
      }
    }

    logger.logBusinessEvent("CHAIN_VALIDATION", Map.of("valid", true, "height", latest().height()));
    return true;
  }

  /**
   * Searches for an evidence by its hash in the entire chain.
   *
   * @param hashHex The SHA-256 hash of the evidence.
   * @return An Optional containing the EvidenceProof (evidence + block) if found.
   */
  public synchronized Optional<EvidenceProof> findEvidenceByHash(String hashHex) {
    var h = hashHex == null ? "" : hashHex.trim().toLowerCase(Locale.ROOT);

    for (Block b : chain) {
      for (EvidenceRecord e : b.evidences()) {
        if (e.hash().equals(h)) {
          logger.logBusinessEvent("EVIDENCE_VERIFIED",
              Map.of("hash", h, "found", true, "blockHeight", b.height()));
          return Optional.of(new EvidenceProof(e, b));
        }
      }
    }
    logger.logBusinessEvent("EVIDENCE_VERIFIED", Map.of("hash", h, "found", false));
    return Optional.empty();
  }

  private Block createGenesis() {
    long height = 0;
    var ts = Instant.parse("2020-01-01T00:00:00Z");
    List<EvidenceRecord> evidences = List.of();
    var previousHash = "0".repeat(64);
    var proposer = "GENESIS";

    var canonical = canonicalBlockFields(height, ts, evidences, previousHash, proposer);
    var hash = hashing.sha256Hex(canonical);

    return new Block(height, ts, evidences, previousHash, proposer, hash, "GENESIS");
  }

  private Block latest() {
    return chain.get(chain.size() - 1);
  }

  private String canonicalBlockFields(long height, Instant ts, List<EvidenceRecord> evidences,
      String previousHash, String proposer) {
    // Deterministic canonicalization (very important)
    var sb = new StringBuilder();
    sb.append("height=").append(height).append("|");
    sb.append("ts=").append(ts.toString()).append("|");
    sb.append("prev=").append(previousHash).append("|");
    sb.append("proposer=").append(proposer).append("|");
    sb.append("evidences=");

    // Deterministic sort by evidenceId
    var sorted = new ArrayList<>(evidences);
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
      var std = e.standards() == null ? List.<String>of() : e.standards();
      var stdSorted = new ArrayList<>(std);
      stdSorted.sort(String::compareTo);
      sb.append(String.join("+", stdSorted));

      sb.append(";");
    }

    return sb.toString();
  }

  private static boolean isValidHexSha256(String hex) {
    if (hex == null) {
      return false;
    }
    var h = hex.trim();
    if (h.length() != 64) {
      return false;
    }
    for (int i = 0; i < h.length(); i++) {
      char c = h.charAt(i);
      boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
      if (!ok) {
        return false;
      }
    }
    return true;
  }

  private static String normalizeAlgo(String algo) {
    return algo == null ? "" : algo.trim().toUpperCase(Locale.ROOT);
  }

  private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    var out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
    }
    return out;
  }

  public record EvidenceProof(EvidenceRecord evidence, Block block) {

  }
}
