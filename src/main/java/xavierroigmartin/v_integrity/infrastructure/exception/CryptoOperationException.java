package xavierroigmartin.v_integrity.infrastructure.exception;

/**
 * Thrown when a cryptographic operation (signing, hashing, verification) fails.
 */
public class CryptoOperationException extends InfrastructureException {
  public CryptoOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
