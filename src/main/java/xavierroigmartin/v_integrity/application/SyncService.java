package xavierroigmartin.v_integrity.application;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import xavierroigmartin.v_integrity.application.port.out.LogPort;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;
import xavierroigmartin.v_integrity.application.port.out.SyncPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.interfaces.rest.dto.BlockHeaderResponse;
import xavierroigmartin.v_integrity.interfaces.rest.dto.SyncResponse;

/**
 * Service responsible for synchronizing the local ledger with peer nodes.
 * <p>
 * Implements the "catch-up" logic:
 * 1. Check latest block height on peer.
 * 2. If peer is ahead, download blocks in batches.
 * 3. Apply blocks sequentially using LedgerService validation.
 */
@Service
public class SyncService {

  private final LedgerService ledgerService;
  private final SyncPort syncPort;
  private final NodeConfigurationPort nodeConfig;
  private final LogPort logger;

  public SyncService(LedgerService ledgerService, SyncPort syncPort,
      NodeConfigurationPort nodeConfig, LogPort logger) {
    this.ledgerService = ledgerService;
    this.syncPort = syncPort;
    this.nodeConfig = nodeConfig;
    this.logger = logger;
  }

  /**
   * Triggers synchronization from a specific peer or the first configured peer.
   *
   * @param sourcePeerUrl Optional peer URL. If null, uses the first peer from config.
   * @return The result of the synchronization process.
   */
  public SyncResponse synchronize(String sourcePeerUrl) {
    String peerUrl = resolvePeerUrl(sourcePeerUrl);
    if (peerUrl == null) {
      return new SyncResponse(false, 0, 0, 0, "No peers configured or provided.");
    }

    // Security check: ensure peer is in the allowed list (if strict)
    // For PoC, we assume config.getPeers() contains trusted URLs.
    // If sourcePeerUrl is provided manually, we should check if it's in the list.
    if (sourcePeerUrl != null && !nodeConfig.getPeers().contains(sourcePeerUrl)) {
       // Depending on strictness. Let's allow it for manual testing but log warning.
       logger.logBusinessEvent("SYNC_WARNING", Map.of("message", "Syncing from unlisted peer", "peer", peerUrl));
    }

    try {
      BlockHeaderResponse remoteHead = syncPort.getLatestBlockHeader(peerUrl);
      long localHeight = ledgerService.latestBlock().height();
      long remoteHeight = remoteHead.height();

      if (remoteHeight <= localHeight) {
        return new SyncResponse(true, 0, localHeight, localHeight, "Already up to date.");
      }

      logger.logBusinessEvent("SYNC_STARTED", Map.of(
          "peer", peerUrl,
          "localHeight", localHeight,
          "remoteHeight", remoteHeight
      ));

      int appliedCount = 0;
      long currentHeight = localHeight;

      while (currentHeight < remoteHeight) {
        long nextHeight = currentHeight + 1;
        // Fetch batch
        List<Block> batch = syncPort.getBlocks(peerUrl, nextHeight, 100);
        if (batch == null || batch.isEmpty()) {
          break; // Should not happen if remoteHeight > localHeight
        }

        for (Block block : batch) {
          ledgerService.acceptReplicatedBlock(block);
          currentHeight = block.height();
          appliedCount++;
        }
      }

      logger.logBusinessEvent("SYNC_COMPLETED", Map.of(
          "applied", appliedCount,
          "finalHeight", currentHeight
      ));

      return new SyncResponse(true, appliedCount, localHeight, currentHeight, null);

    } catch (Exception e) {
      logger.logBusinessError("SYNC_FAILED", e.getMessage(), Map.of("peer", peerUrl));
      return new SyncResponse(false, 0, 0, 0, "Sync failed: " + e.getMessage());
    }
  }

  private String resolvePeerUrl(String input) {
    if (input != null && !input.isBlank()) {
      return input;
    }
    List<String> peers = nodeConfig.getPeers();
    if (peers != null && !peers.isEmpty()) {
      return peers.get(0);
    }
    return null;
  }
}
