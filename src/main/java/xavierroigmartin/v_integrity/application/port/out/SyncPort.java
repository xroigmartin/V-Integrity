package xavierroigmartin.v_integrity.application.port.out;

import java.util.List;
import xavierroigmartin.v_integrity.domain.Block;
import xavierroigmartin.v_integrity.interfaces.rest.dto.BlockHeaderResponse;

/**
 * Port for synchronizing blocks from other nodes.
 * <p>
 * Allows fetching the latest block header and downloading ranges of blocks from peers.
 */
public interface SyncPort {

  /**
   * Fetches the latest block header from a peer.
   *
   * @param peerUrl The base URL of the peer.
   * @return The latest block header info.
   */
  BlockHeaderResponse getLatestBlockHeader(String peerUrl);

  /**
   * Downloads a list of blocks from a peer.
   *
   * @param peerUrl    The base URL of the peer.
   * @param fromHeight The starting height (inclusive).
   * @param limit      Maximum number of blocks to fetch.
   * @return List of blocks.
   */
  List<Block> getBlocks(String peerUrl, long fromHeight, int limit);
}
