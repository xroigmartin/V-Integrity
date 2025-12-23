package xavierroigmartin.v_integrity.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * Port to access the node's configuration.
 * <p>
 * The application layer needs to know "who am I" (nodeId), "am I a leader?", and "who are my peers?".
 * This port decouples the domain from Spring's configuration properties or environment variables.
 */
public interface NodeConfigurationPort {

    /**
     * @return The unique identifier of this node (e.g., "node-1").
     */
    String getNodeId();

    /**
     * @return true if this node is authorized to create (mine) new blocks.
     */
    boolean isLeader();

    /**
     * @return A list of base URLs of other peer nodes for replication.
     */
    List<String> getPeers();

    /**
     * @return The private key of this node (if leader) for signing blocks.
     */
    String getPrivateKeyBase64();

    /**
     * @return A map of authorized node IDs to their Public Keys. Used to validate block proposers.
     */
    Map<String, String> getAllowedNodePublicKeys();
}
