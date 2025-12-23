package xavierroigmartin.v_integrity.application.port.out;

import xavierroigmartin.v_integrity.domain.Block;
import java.util.List;

/**
 * Port for replicating blocks to other nodes in the network.
 * <p>
 * In a distributed ledger, when a new block is created, it must be propagated to all peers
 * so they can validate and append it to their local chain.
 */
public interface ReplicationPort {

    /**
     * Sends a newly created block to a list of peer nodes.
     *
     * @param block        The block to replicate.
     * @param peerBaseUrls The list of peer URLs to send the block to.
     */
    void replicateBlockToPeers(Block block, List<String> peerBaseUrls);
}
