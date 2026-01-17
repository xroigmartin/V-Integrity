package xavierroigmartin.v_integrity.infrastructure.exception;

import xavierroigmartin.v_integrity.application.exception.AppErrorCodes;

/**
 * Thrown when the ledger rehydration process fails due to technical issues or corruption.
 */
public class RehydrationFailedException extends InfrastructureException {
  public RehydrationFailedException(String message, Throwable cause) {
    super(message, cause, AppErrorCodes.ERR_INTERNAL_ERROR);
  }
}
