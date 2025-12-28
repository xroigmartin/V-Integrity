package xavierroigmartin.v_integrity.infrastructure.adapter;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import xavierroigmartin.v_integrity.domain.Block;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class ReplicationAdapterTest {

  private ReplicationAdapter replicationAdapter;
  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    // Bind mock server FIRST to configure the builder with a mock request factory
    mockServer = MockRestServiceServer.bindTo(builder).build();
    // THEN build the client using the configured builder
    replicationAdapter = new ReplicationAdapter(builder);
  }

  @Test
  void should_replicate_block_successfully() {
    // Given
    Block block = createSampleBlock();
    String peerUrl = "http://peer1:8080";

    mockServer.expect(requestTo(peerUrl + "/api/blocks/receive"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    // When
    replicationAdapter.replicateBlockToPeers(block, List.of(peerUrl));

    // Then
    mockServer.verify();
  }

  @Test
  void should_handle_replication_error_gracefully() {
    // Given
    Block block = createSampleBlock();
    String peerUrl = "http://peer1:8080";

    mockServer.expect(requestTo(peerUrl + "/api/blocks/receive"))
        .andRespond(withServerError());

    // When
    // Should not throw exception (logs error instead)
    replicationAdapter.replicateBlockToPeers(block, List.of(peerUrl));

    // Then
    mockServer.verify();
  }

  private Block createSampleBlock() {
    return new Block(1, Instant.now(), List.of(), "prev", "node1", "hash", "sig");
  }
}
