package xavierroigmartin.v_integrity.infrastructure.adapter;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import xavierroigmartin.v_integrity.application.SyncService;
import xavierroigmartin.v_integrity.application.port.out.LogPort;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;
import xavierroigmartin.v_integrity.interfaces.rest.dto.SyncResponse;

/**
 * Infrastructure adapter that triggers automatic synchronization upon application startup.
 * <p>
 * Listens for the {@link ApplicationReadyEvent} and iterates through all configured peers
 * to attempt a catch-up. This ensures the node has the latest state before processing new requests.
 */
@Component
@Profile("!test") // Avoid running this during unit/integration tests unless explicitly desired
public class AutoSyncAdapter implements ApplicationListener<ApplicationReadyEvent> {

  private final SyncService syncService;
  private final NodeConfigurationPort nodeConfig;
  private final LogPort logger;

  public AutoSyncAdapter(SyncService syncService, NodeConfigurationPort nodeConfig, LogPort logger) {
    this.syncService = syncService;
    this.nodeConfig = nodeConfig;
    this.logger = logger;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    // In this PoC with a single Authority Leader, the Leader is the source of truth.
    // It should not attempt to sync from followers on startup to avoid connection errors
    // if followers are not yet up.
    if (nodeConfig.isLeader()) {
      logger.logBusinessEvent("AUTO_SYNC_SKIPPED", Map.of("reason", "Leader node does not sync on startup"));
      return;
    }

    List<String> peers = nodeConfig.getPeers();

    if (peers == null || peers.isEmpty()) {
      logger.logBusinessEvent("AUTO_SYNC_SKIPPED", Map.of("reason", "No peers configured"));
      return;
    }

    logger.logBusinessEvent("AUTO_SYNC_STARTED", Map.of("peerCount", peers.size()));

    for (String peerUrl : peers) {
      try {
        // Attempt to sync with each peer to get the most up-to-date chain
        SyncResponse response = syncService.synchronize(peerUrl);
        
        if (response.synced() && response.appliedBlocks() > 0) {
          logger.logBusinessEvent("AUTO_SYNC_SUCCESS", Map.of(
              "peer", peerUrl,
              "appliedBlocks", response.appliedBlocks(),
              "currentHeight", response.toHeight()
          ));
        } else if (!response.synced()) {
           // Log warning but continue to next peer
           logger.logBusinessEvent("AUTO_SYNC_PEER_FAILED", Map.of(
               "peer", peerUrl, 
               "reason", response.reason()
           ));
        }
      } catch (Exception e) {
        logger.logBusinessError("AUTO_SYNC_ERROR", "Failed to sync with peer during startup", 
            Map.of("peer", peerUrl, "error", e.getMessage()));
      }
    }

    logger.logBusinessEvent("AUTO_SYNC_FINISHED", Map.of("nodeId", nodeConfig.getNodeId()));
  }
}
