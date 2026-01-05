package xavierroigmartin.v_integrity.domain.exception;

import xavierroigmartin.v_integrity.application.exception.AppErrorCodes;

/**
 * Thrown when a block fails validation rules (hash mismatch, invalid signature, bad height, etc.).
 */
public class InvalidBlockException extends DomainException {
  public InvalidBlockException(String message) {
    super(message, AppErrorCodes.ERR_BLOCK_INVALID);
  }
}
