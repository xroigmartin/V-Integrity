package xavierroigmartin.v_integrity.application.port.out;

import xavierroigmartin.v_integrity.domain.Block;
import java.util.List;

public interface ReplicationPort {
    void replicateBlockToPeers(Block block, List<String> peerBaseUrls);
}
