package xavierroigmartin.v_integrity;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import xavierroigmartin.v_integrity.infrastructure.config.NodeProperties;

@SpringBootApplication
@EnableConfigurationProperties(NodeProperties.class)
public class VIntegrityApplication {

  private static final Logger logger = LoggerFactory.getLogger(VIntegrityApplication.class);

  @Value("${ledger.node.privateKeyBase64:}")
  private String privateKeyCheck;

  @Value("${ledger.node.nodeId:unknown}")
  private String nodeId;

  @Value("${ledger.node.leader:false}")
  private boolean isLeader;

  static void main(String[] args) {
    SpringApplication.run(VIntegrityApplication.class, args);
  }

  @PostConstruct
  public void init() {
    // Set JVM timezone to UTC
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    // Set Node ID in MDC for all logs (System & Business)
    MDC.put("node_id", nodeId);

    logger.info(">>> STARTING V-INTEGRITY NODE: {} (Leader: {}) <<<", nodeId, isLeader);

    if (privateKeyCheck == null || privateKeyCheck.isBlank()) {
      if (isLeader) {
        logger.error(">>> CRITICAL: ledger.node.privateKeyBase64 IS MISSING. Leader node cannot sign blocks! <<<");
      } else {
        logger.warn(">>> WARNING: ledger.node.privateKeyBase64 is not set. This is fine for follower nodes. <<<");
      }
    } else {
      logger.info(">>> SUCCESS: Private Key loaded (length: {} chars) <<<", privateKeyCheck.length());
    }
  }
}
