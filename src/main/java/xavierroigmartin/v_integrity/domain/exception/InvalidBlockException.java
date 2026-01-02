package xavierroigmartin.v_integrity.domain.exception;

/**
 * Thrown when a block fails validation rules (hash mismatch, invalid signature, bad height, etc.).
 */
public class InvalidBlockException extends DomainException {
  public InvalidBlockException(String message) {
    super(message);
  }
}
