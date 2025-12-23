package xavierroigmartin.v_integrity.infrastructure.adapter;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import xavierroigmartin.v_integrity.application.port.out.ReplicationPort;
import xavierroigmartin.v_integrity.domain.Block;

import java.util.List;

/**
 * Implementation of {@link ReplicationPort} using Spring's {@link RestClient}.
 * <p>
 * Sends HTTP POST requests to peer nodes to propagate blocks.
 */
@Component
public class ReplicationAdapter implements ReplicationPort {

    private final RestClient rest = RestClient.create();

    @Override
    public void replicateBlockToPeers(Block block, List<String> peerBaseUrls) {
        for (String base : peerBaseUrls) {
            try {
                rest.post()
                        .uri(base + "/api/blocks/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(block)
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                // PoC: best-effort replication.
                // In a production system, we should implement retries, Dead Letter Queues (DLQ),
                // metrics, and exponential backoff.
                System.err.println("[REPLICATION] Error replicating to " + base + ": " + e.getMessage());
            }
        }
    }
}
