package xavierroigmartin.v_integrity.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledger.node")
public class NodeProperties {

    /**
     * Identidad del nodo actual.
     */
    private String nodeId;

    /**
     * true si este nodo es el líder/autoridad para hacer commit.
     */
    private boolean leader;

    /**
     * URL base de peers (p.ej. http://localhost:8082).
     */
    private List<String> peers = List.of();

    /**
     * Clave privada Ed25519 en Base64 (solo necesaria si leader=true).
     */
    private String privateKeyBase64;

    /**
     * Mapa nodeId -> publicKeyBase64 (allowlist de autoridades/validadores).
     */
    private Map<String, String> allowedNodePublicKeys = Map.of();

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public boolean isLeader() { return leader; }
    public void setLeader(boolean leader) { this.leader = leader; }

    public List<String> getPeers() { return peers; }
    public void setPeers(List<String> peers) { this.peers = peers; }

    public String getPrivateKeyBase64() { return privateKeyBase64; }
    public void setPrivateKeyBase64(String privateKeyBase64) { this.privateKeyBase64 = privateKeyBase64; }

    public Map<String, String> getAllowedNodePublicKeys() { return allowedNodePublicKeys; }
    public void setAllowedNodePublicKeys(Map<String, String> allowedNodePublicKeys) { this.allowedNodePublicKeys = allowedNodePublicKeys; }
}
