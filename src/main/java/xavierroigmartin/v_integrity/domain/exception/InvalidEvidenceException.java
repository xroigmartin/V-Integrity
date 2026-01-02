package xavierroigmartin.v_integrity.domain.exception;

/**
 * Thrown when an evidence record is invalid or malformed.
 */
public class InvalidEvidenceException extends DomainException {
  public InvalidEvidenceException(String message) {
    super(message);
  }
}
