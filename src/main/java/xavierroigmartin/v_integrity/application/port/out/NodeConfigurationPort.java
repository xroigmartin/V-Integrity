package xavierroigmartin.v_integrity.application.port.out;

import java.util.List;
import java.util.Map;

public interface NodeConfigurationPort {
    String getNodeId();
    boolean isLeader();
    List<String> getPeers();
    String getPrivateKeyBase64();
    Map<String, String> getAllowedNodePublicKeys();
}
