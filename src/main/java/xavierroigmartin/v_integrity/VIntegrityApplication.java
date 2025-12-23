package xavierroigmartin.v_integrity;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	public static void main(String[] args) {
		SpringApplication.run(VIntegrityApplication.class, args);
	}

	@PostConstruct
	public void init() {
		// Set JVM timezone to UTC
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		
		if (privateKeyCheck == null || privateKeyCheck.isBlank()) {
			logger.error(">>> ERROR: ledger.node.privateKeyBase64 IS NOT SET OR EMPTY <<<");
			logger.error("Check your environment variable LEDGER_PRIVATE_KEY_BASE64");
		} else {
			logger.info(">>> SUCCESS: ledger.node.privateKeyBase64 is configured (length: {}) <<<", privateKeyCheck.length());
		}
	}

}
