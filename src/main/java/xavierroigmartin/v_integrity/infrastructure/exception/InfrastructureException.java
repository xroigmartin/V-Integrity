package xavierroigmartin.v_integrity.infrastructure.exception;

import xavierroigmartin.v_integrity.application.exception.AppErrorCodes;

/**
 * Base exception for infrastructure-related errors.
 * Indicates failures in external adapters (crypto, network, IO).
 */
public abstract class InfrastructureException extends RuntimeException {
  
  private final String errorCode;

  public InfrastructureException(String message) {
    this(message, null, AppErrorCodes.ERR_INFRASTRUCTURE);
  }

  public InfrastructureException(String message, Throwable cause) {
    this(message, cause, AppErrorCodes.ERR_INFRASTRUCTURE);
  }

  public InfrastructureException(String message, String errorCode) {
    this(message, null, errorCode);
  }

  public InfrastructureException(String message, Throwable cause, String errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
