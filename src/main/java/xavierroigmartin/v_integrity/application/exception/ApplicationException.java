package xavierroigmartin.v_integrity.application.exception;

/**
 * Base exception for application service errors.
 * Indicates issues with the flow, permissions, or state of the application.
 */
public abstract class ApplicationException extends RuntimeException {
  public ApplicationException(String message) {
    super(message);
  }

  public ApplicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
