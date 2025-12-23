package xavierroigmartin.v_integrity.application;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import xavierroigmartin.v_integrity.domain.Block;

@Service
public class ReplicationService {

    private final RestClient rest = RestClient.create();

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
                // PoC: best-effort. En real: retries, DLQ, métricas, backoff, etc.
                System.err.println("[REPLICATION] Error replicando a " + base + ": " + e.getMessage());
            }
        }
    }
}