package xavierroigmartin.v_integrity.infrastructure.adapter;

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import xavierroigmartin.v_integrity.application.port.out.SyncPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.interfaces.rest.dto.BlockHeaderResponse;

/**
 * Implementation of {@link SyncPort} using Spring's {@link RestClient}.
 */
@Component
public class SyncAdapter implements SyncPort {

  private final RestClient restClient;

  public SyncAdapter(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  @Override
  public BlockHeaderResponse getLatestBlockHeader(String peerUrl) {
    return restClient.get()
        .uri(peerUrl + "/api/blocks/latest")
        .retrieve()
        .body(BlockHeaderResponse.class);
  }

  @Override
  public List<Block> getBlocks(String peerUrl, long fromHeight, int limit) {
    return restClient.get()
        .uri(peerUrl + "/api/blocks?fromHeight={fromHeight}&limit={limit}", fromHeight, limit)
        .retrieve()
        .body(new ParameterizedTypeReference<List<Block>>() {});
  }
}
