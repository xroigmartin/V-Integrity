package xavierroigmartin.v_integrity.infrastructure.adapter;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xavierroigmartin.v_integrity.application.port.out.LogPort;

/**
 * Implementation of {@link LogPort} using SLF4J.
 * <p>
 * Writes to a specific logger named "business-logger", which is configured
 * in logback-spring.xml to write to a separate JSON file.
 */
@Component
public class LogAdapter implements LogPort {

  private static final Logger BUSINESS_LOGGER = LoggerFactory.getLogger("business-logger");

  @Override
  public void logBusinessEvent(String event, Map<String, Object> details) {
    // We use structured logging arguments if available, or just format it nicely.
    // With LogstashEncoder, we can use Markers, but for simplicity in this PoC,
    // we'll log a formatted message. Ideally, we would use StructuredArguments.
    BUSINESS_LOGGER.info("Event: {} | Details: {}", event, details);
  }

  @Override
  public void logBusinessError(String error, String message, Map<String, Object> details) {
    BUSINESS_LOGGER.error("Error: {} | Message: {} | Details: {}", error, message, details);
  }
}
