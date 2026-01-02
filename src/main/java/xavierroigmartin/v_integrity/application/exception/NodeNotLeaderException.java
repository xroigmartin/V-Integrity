package xavierroigmartin.v_integrity.application.exception;

/**
 * Thrown when a non-leader node attempts to perform a leader-only operation (e.g., commit block).
 */
public class NodeNotLeaderException extends ApplicationException {
  public NodeNotLeaderException(String message) {
    super(message);
  }
}
