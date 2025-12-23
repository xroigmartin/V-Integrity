package xavierroigmartin.v_integrity.infrastructure.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import xavierroigmartin.v_integrity.application.port.out.NodeConfigurationPort;

@ConfigurationProperties(prefix = "ledger.node")
public class NodeProperties implements NodeConfigurationPort {

    private String nodeId;
    private boolean leader;
    private List<String> peers = List.of();
    private String privateKeyBase64;
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
