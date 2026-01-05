package xavierroigmartin.v_integrity.domain.exception;

import xavierroigmartin.v_integrity.application.exception.AppErrorCodes;

/**
 * Base exception for all domain-related errors.
 * Indicates a violation of business rules or invariants.
 */
public abstract class DomainException extends RuntimeException {
  
  private final String errorCode;

  public DomainException(String message) {
    this(message, AppErrorCodes.ERR_DOMAIN_RULE);
  }

  public DomainException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public DomainException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = AppErrorCodes.ERR_DOMAIN_RULE;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
