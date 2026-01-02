package xavierroigmartin.v_integrity.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import xavierroigmartin.v_integrity.application.LedgerService;
import xavierroigmartin.v_integrity.application.SyncService;
import xavierroigmartin.v_integrity.application.port.out.CryptoPort;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.domain.EvidenceRecord;
import xavierroigmartin.v_integrity.interfaces.rest.dto.BlockHeaderResponse;
import xavierroigmartin.v_integrity.interfaces.rest.dto.EvidenceRequest;
import xavierroigmartin.v_integrity.interfaces.rest.dto.SyncRequest;
import xavierroigmartin.v_integrity.interfaces.rest.dto.SyncResponse;
import xavierroigmartin.v_integrity.interfaces.rest.dto.VerifyRequest;

/**
 * REST Controller exposing the blockchain ledger functionality.
 * <p>
 * Provides endpoints for:
 * <ul>
 *     <li>Querying the chain and mempool state.</li>
 *     <li>Submitting new evidences.</li>
 *     <li>Committing blocks (Leader only).</li>
 *     <li>Receiving replicated blocks (Followers).</li>
 *     <li>Verifying evidences and chain integrity.</li>
 *     <li>Synchronizing with other nodes.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Ledger API", description = "Operations related to the blockchain ledger, evidences, and blocks.")
public class LedgerController {

  private final LedgerService ledger;
  private final SyncService syncService;
  private final CryptoPort crypto;
  private final NodeConfigurationPort nodeConfig;

  public LedgerController(LedgerService ledger, SyncService syncService, CryptoPort crypto, NodeConfigurationPort nodeConfig) {
    this.ledger = ledger;
    this.syncService = syncService;
    this.crypto = crypto;
    this.nodeConfig = nodeConfig;
  }

  /**
   * Retrieves the full blockchain.
   *
   * @return A map containing the chain length and the list of blocks.
   */
  @Operation(summary = "Get Blockchain", description = "Retrieves the full list of blocks in the local chain.")
  @ApiResponse(responseCode = "200", description = "Chain retrieved successfully")
  @GetMapping("/chain")
  public Map<String, Object> chain() {
    return Map.of("length", ledger.chain().size(), "chain", ledger.chain());
  }

  /**
   * Retrieves the latest block header information.
   * Useful for peers to check if they are behind.
   *
   * @return The latest block header.
   */
  @Operation(summary = "Get Latest Block Header", description = "Returns the header of the latest block.")
  @GetMapping("/blocks/latest")
  public BlockHeaderResponse getLatestBlock() {
    Block latest = ledger.latestBlock();
    return new BlockHeaderResponse(
        latest.height(),
        latest.hash(),
        latest.proposerNodeId(),
        latest.timestamp()
    );
  }

  /**
   * Retrieves a range of blocks.
   * Used by peers during synchronization.
   *
   * @param fromHeight Starting height (inclusive).
   * @param limit      Max number of blocks (default 100).
   * @return List of blocks.
   */
  @Operation(summary = "Get Blocks Range", description = "Returns a list of blocks starting from a specific height.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Blocks retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid parameters (e.g., negative fromHeight)")
  })
  @GetMapping("/blocks")
  public List<Block> getBlocks(
      @RequestParam(defaultValue = "0") long fromHeight,
      @RequestParam(defaultValue = "100") int limit) {
    return ledger.getBlocksFromHeight(fromHeight, limit);
  }

  /**
   * Triggers a manual synchronization process.
   *
   * @param req Optional request containing the source peer URL.
   * @return The result of the synchronization.
   */
  @Operation(summary = "Trigger Sync", description = "Manually triggers synchronization with a peer.")
  @PostMapping("/sync")
  public SyncResponse sync(@RequestBody(required = false) SyncRequest req) {
    String peerUrl = (req != null) ? req.sourcePeerUrl() : null;
    return syncService.synchronize(peerUrl);
  }

  /**
   * Retrieves the current mempool (pending evidences).
   *
   * @return A map containing the mempool size and the list of pending evidences.
   */
  @Operation(summary = "Get Mempool", description = "Retrieves the list of pending evidences waiting to be mined.")
  @ApiResponse(responseCode = "200", description = "Mempool retrieved successfully")
  @GetMapping("/mempool")
  public Map<String, Object> mempool() {
    return Map.of("size", ledger.mempool().size(), "mempool", ledger.mempool());
  }

  /**
   * Submits a new evidence to be secured in the blockchain.
   *
   * @param req The evidence request containing artifact metadata and hash.
   * @return A map containing the stored evidence record.
   */
  @Operation(summary = "Submit Evidence", description = "Registers a new evidence in the node's mempool.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Evidence created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input data")
  })
  @PostMapping("/evidences")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> submitEvidence(@Valid @RequestBody EvidenceRequest req) {
    EvidenceRecord evidence = new EvidenceRecord(
        null,
        req.homologationId(),
        req.testRunId(),
        req.artifactName(),
        req.artifactType(),
        req.hashAlgorithm(),
        req.hash(),
        req.sizeBytes(),
        req.createdBy(),
        req.storageUri(),
        req.standards(),
        null
    );
    EvidenceRecord stored = ledger.submitEvidence(evidence);
    return Map.of("evidence", stored);
  }

  /**
   * Triggers the block creation process (mining/commit).
   * <p>
   * Only available if the current node is configured as a LEADER.
   *
   * @return A map containing the newly created block.
   */
  @Operation(summary = "Commit Block (Leader Only)", description = "Triggers the creation of a new block containing all pending evidences.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Block mined and committed successfully"),
      @ApiResponse(responseCode = "400", description = "Node is not a leader or mempool is empty")
  })
  @PostMapping("/blocks/commit")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> commit() {
    Block block = ledger.commitAsLeader();
    return Map.of("block", block);
  }

  /**
   * Endpoint for receiving blocks replicated from other nodes.
   *
   * @param incoming The block received from a peer.
   * @return A map indicating acceptance and the block height.
   */
  @Operation(summary = "Receive Replicated Block", description = "Internal endpoint for receiving blocks from peer nodes.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Block accepted"),
      @ApiResponse(responseCode = "400", description = "Invalid block (hash mismatch, bad signature)")
  })
  @PostMapping("/blocks/receive")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, Object> receive(@RequestBody Block incoming) {
    ledger.acceptReplicatedBlock(incoming);
    return Map.of("accepted", true, "height", incoming.height());
  }

  /**
   * Validates the integrity of the local blockchain.
   *
   * @return A map with "valid": true/false.
   */
  @Operation(summary = "Validate Chain", description = "Checks the cryptographic integrity of the local chain.")
  @ApiResponse(responseCode = "200", description = "Validation result returned")
  @GetMapping("/validate")
  public Map<String, Object> validate() {
    return Map.of("valid", ledger.isValidLocalChain());
  }

  /**
   * Direct query by hash (useful for debug / demos).
   *
   * @param hash The SHA-256 hash of the evidence.
   * @return A map with "found": true/false and the evidence details if found.
   */
  @Operation(summary = "Get Evidence by Hash", description = "Direct lookup of an evidence by its hash.")
  @ApiResponse(responseCode = "200", description = "Search completed (check 'found' field)")
  @GetMapping("/evidences/hash/{hash}")
  public Map<String, Object> getEvidenceByHash(
      @Parameter(description = "SHA-256 hash of the evidence") @PathVariable String hash) {
    Optional<LedgerService.EvidenceProof> found = ledger.findEvidenceByHash(hash);
    return found.map(evidenceProof -> Map.of(
        "found", true,
        "evidence", evidenceProof.evidence(),
        "proof", proofFrom(evidenceProof)
    )).orElseGet(() -> Map.of("found", false));
  }

  /**
   * Verification by hash: returns an audit-oriented proof.
   *
   * @param req The verification request containing the hash.
   * @return A map containing verification status and cryptographic proof details.
   */
  @Operation(summary = "Verify Evidence", description = "Verifies if a specific hash exists and returns a cryptographic proof.")
  @ApiResponse(responseCode = "200", description = "Verification result returned")
  @PostMapping("/verify")
  public Map<String, Object> verify(@Valid @RequestBody VerifyRequest req) {
    String hash = req.hash().trim().toLowerCase(Locale.ROOT);
    Optional<LedgerService.EvidenceProof> found = ledger.findEvidenceByHash(hash);

    if (found.isEmpty()) {
      return Map.of(
          "verified", false,
          "reason", "NOT_FOUND",
          "hash", hash
      );
    }

    Map<String, Object> proof = proofFrom(found.get());
    return Map.of(
        "verified", true,
        "hash", hash,
        "evidence", found.get().evidence(),
        "proof", proof
    );
  }

  private Map<String, Object> proofFrom(LedgerService.EvidenceProof ep) {
    Block b = ep.block();

    // Explicit signature validation (for "proof" in demo)
    String pubKey = nodeConfig.getAllowedNodePublicKeys().get(b.proposerNodeId());
    boolean signatureValid = false;
    if (pubKey != null && !pubKey.isBlank() && !"GENESIS".equals(b.proposerNodeId())) {
      signatureValid = crypto.verifyEd25519(hexToBytes(b.hash()), b.signature(), pubKey);
    }

    return Map.of(
        "blockHeight", b.height(),
        "blockTimestamp", b.timestamp().toString(),
        "blockHash", b.hash(),
        "previousHash", b.previousHash(),
        "signedBy", b.proposerNodeId(),
        "signatureValid", signatureValid
    );
  }

  private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
    }
    return out;
  }
}
