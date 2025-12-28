package xavierroigmartin.v_integrity.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xavierroigmartin.v_integrity.application.port.out.LogPort;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;
import xavierroigmartin.v_integrity.application.port.out.SyncPort;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.interfaces.rest.dto.BlockHeaderResponse;
import xavierroigmartin.v_integrity.interfaces.rest.dto.SyncResponse;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

  @Mock
  private LedgerService ledgerService;
  @Mock
  private SyncPort syncPort;
  @Mock
  private NodeConfigurationPort nodeConfig;
  @Mock
  private LogPort logger;

  private SyncService syncService;

  @BeforeEach
  void setUp() {
    syncService = new SyncService(ledgerService, syncPort, nodeConfig, logger);
  }

  @Test
  void should_sync_when_remote_is_ahead() {
    // Given
    String peerUrl = "http://peer1";
    when(nodeConfig.getPeers()).thenReturn(List.of(peerUrl));

    // Local chain at height 0 (Genesis)
    Block localHead = createBlock(0);
    when(ledgerService.latestBlock()).thenReturn(localHead);

    // Remote chain at height 2
    BlockHeaderResponse remoteHead = new BlockHeaderResponse(2, "hash2", "node2", Instant.now());
    when(syncPort.getLatestBlockHeader(peerUrl)).thenReturn(remoteHead);

    // Blocks to download
    Block block1 = createBlock(1);
    Block block2 = createBlock(2);
    when(syncPort.getBlocks(peerUrl, 1, 100)).thenReturn(List.of(block1, block2));

    // When
    SyncResponse response = syncService.synchronize(null);

    // Then
    assertTrue(response.synced());
    assertEquals(2, response.appliedBlocks());
    assertEquals(0, response.fromHeight());
    assertEquals(2, response.toHeight());

    verify(ledgerService).acceptReplicatedBlock(block1);
    verify(ledgerService).acceptReplicatedBlock(block2);
  }

  @Test
  void should_do_nothing_if_already_synced() {
    // Given
    String peerUrl = "http://peer1";
    when(nodeConfig.getPeers()).thenReturn(List.of(peerUrl));

    Block localHead = createBlock(5);
    when(ledgerService.latestBlock()).thenReturn(localHead);

    BlockHeaderResponse remoteHead = new BlockHeaderResponse(5, "hash5", "node2", Instant.now());
    when(syncPort.getLatestBlockHeader(peerUrl)).thenReturn(remoteHead);

    // When
    SyncResponse response = syncService.synchronize(null);

    // Then
    assertTrue(response.synced());
    assertEquals(0, response.appliedBlocks());
    assertEquals(5, response.fromHeight());
    assertEquals(5, response.toHeight());

    verify(syncPort, times(0)).getBlocks(anyString(), anyLong(), anyInt());
  }

  @Test
  void should_handle_sync_failure() {
    // Given
    String peerUrl = "http://peer1";
    when(nodeConfig.getPeers()).thenReturn(List.of(peerUrl));

    Block localHead = createBlock(0);
    when(ledgerService.latestBlock()).thenReturn(localHead);

    BlockHeaderResponse remoteHead = new BlockHeaderResponse(1, "hash1", "node2", Instant.now());
    when(syncPort.getLatestBlockHeader(peerUrl)).thenReturn(remoteHead);

    Block block1 = createBlock(1);
    when(syncPort.getBlocks(peerUrl, 1, 100)).thenReturn(List.of(block1));

    // Simulate validation error
    doThrow(new IllegalArgumentException("Invalid block")).when(ledgerService).acceptReplicatedBlock(block1);

    // When
    SyncResponse response = syncService.synchronize(null);

    // Then
    assertFalse(response.synced());
    assertTrue(response.reason().contains("Invalid block"));
  }

  private Block createBlock(long height) {
    return new Block(height, Instant.now(), List.of(), "prev", "node", "hash" + height, "sig");
  }
}
