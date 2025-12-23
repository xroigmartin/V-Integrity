package xavierroigmartin.v_integrity.application.port.out;

import java.util.Map;

/**
 * Port for structured business logging.
 * <p>
 * Decouples the domain from the logging framework (SLF4J/Logback).
 * Allows recording audit events and business milestones.
 */
public interface LogPort {

  /**
   * Logs a business event.
   *
   * @param event The name of the event (e.g., "EVIDENCE_SUBMITTED").
   * @param details A map of key-value pairs with event details.
   */
  void logBusinessEvent(String event, Map<String, Object> details);

  /**
   * Logs a business error or anomaly.
   *
   * @param error The error code or name.
   * @param message A descriptive message.
   * @param details Context details.
   */
  void logBusinessError(String error, String message, Map<String, Object> details);
}
