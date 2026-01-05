package xavierroigmartin.v_integrity.application.exception;

/**
 * Base exception for application service errors.
 * Indicates issues with the flow, permissions, or state of the application.
 */
public abstract class ApplicationException extends RuntimeException {
  
  private final String errorCode;

  public ApplicationException(String message) {
    this(message, AppErrorCodes.ERR_APPLICATION_STATE);
  }

  public ApplicationException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public ApplicationException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = AppErrorCodes.ERR_APPLICATION_STATE;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
