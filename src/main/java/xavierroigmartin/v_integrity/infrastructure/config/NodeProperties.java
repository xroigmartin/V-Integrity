package xavierroigmartin.v_integrity.infrastructure.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;

/**
 * Configuration properties for the blockchain node.
 * <p>
 * Maps properties starting with "ledger.node" from application.yaml/properties.
 * Implements {@link NodeConfigurationPort} to provide configuration to the application layer.
 */
@ConfigurationProperties(prefix = "ledger.node")
public class NodeProperties implements NodeConfigurationPort {

    /**
     * Unique identifier for this node.
     */
    private String nodeId;

    /**
     * True if this node is a leader/authority allowed to commit blocks.
     */
    private boolean leader;

    /**
     * List of peer URLs (e.g., http://localhost:8082) for replication.
     */
    private List<String> peers = List.of();

    /**
     * Ed25519 Private Key in Base64 (PKCS#8). Required only if leader=true.
     */
    private String privateKeyBase64;

    /**
     * Map of nodeId -> publicKeyBase64 (X.509). Allowlist of authorized validators.
     */
    private Map<String, String> allowedNodePublicKeys = Map.of();

    @Override
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    @Override
    public boolean isLeader() { return leader; }
    public void setLeader(boolean leader) { this.leader = leader; }

    @Override
    public List<String> getPeers() { return peers; }
    public void setPeers(List<String> peers) { this.peers = peers; }

    @Override
    public String getPrivateKeyBase64() { return privateKeyBase64; }
    public void setPrivateKeyBase64(String privateKeyBase64) { this.privateKeyBase64 = privateKeyBase64; }

    @Override
    public Map<String, String> getAllowedNodePublicKeys() { return allowedNodePublicKeys; }
    public void setAllowedNodePublicKeys(Map<String, String> allowedNodePublicKeys) { this.allowedNodePublicKeys = allowedNodePublicKeys; }
}
