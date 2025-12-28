package xavierroigmartin.v_integrity.infrastructure.adapter;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import xavierroigmartin.v_integrity.application.port.out.ReplicationPort;
import xavierroigmartin.v_integrity.domain.Block;

/**
 * Implementation of {@link ReplicationPort} using Spring's {@link RestClient}.
 * <p>
 * Sends HTTP POST requests to peer nodes to propagate blocks.
 */
@Component
public class ReplicationAdapter implements ReplicationPort {

  private static final Logger logger = LoggerFactory.getLogger(ReplicationAdapter.class);
  private final RestClient restClient;

  public ReplicationAdapter(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  @Override
  public void replicateBlockToPeers(Block block, List<String> peerBaseUrls) {
    for (String base : peerBaseUrls) {
      try {
        logger.info("Replicating block height={} to peer: {}", block.height(), base);
        restClient.post()
            .uri(base + "/api/blocks/receive")
            .contentType(MediaType.APPLICATION_JSON)
            .body(block)
            .retrieve()
            .toBodilessEntity();
        logger.debug("Successfully replicated block height={} to {}", block.height(), base);
      } catch (Exception e) {
        // PoC: best-effort replication.
        logger.error("Failed to replicate block height={} to {}: {}", block.height(), base, e.getMessage());
      }
    }
  }
}
