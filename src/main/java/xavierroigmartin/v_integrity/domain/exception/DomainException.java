package xavierroigmartin.v_integrity.domain.exception;

/**
 * Base exception for all domain-related errors.
 * Indicates a violation of business rules or invariants.
 */
public abstract class DomainException extends RuntimeException {
  public DomainException(String message) {
    super(message);
  }

  public DomainException(String message, Throwable cause) {
    super(message, cause);
  }
}
