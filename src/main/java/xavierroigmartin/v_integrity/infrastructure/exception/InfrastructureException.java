package xavierroigmartin.v_integrity.infrastructure.exception;

/**
 * Base exception for infrastructure-related errors.
 * Indicates failures in external adapters (crypto, network, IO).
 */
public abstract class InfrastructureException extends RuntimeException {
  public InfrastructureException(String message) {
    super(message);
  }

  public InfrastructureException(String message, Throwable cause) {
    super(message, cause);
  }
}
