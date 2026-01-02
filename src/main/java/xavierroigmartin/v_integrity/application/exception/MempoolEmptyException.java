package xavierroigmartin.v_integrity.application.exception;

/**
 * Thrown when attempting to commit a block but there are no pending evidences.
 */
public class MempoolEmptyException extends ApplicationException {
  public MempoolEmptyException(String message) {
    super(message);
  }
}
