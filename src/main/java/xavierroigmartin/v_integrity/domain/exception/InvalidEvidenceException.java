package xavierroigmartin.v_integrity.domain.exception;

import xavierroigmartin.v_integrity.application.exception.AppErrorCodes;

/**
 * Thrown when an evidence record is invalid or malformed.
 */
public class InvalidEvidenceException extends DomainException {
  public InvalidEvidenceException(String message) {
    super(message, AppErrorCodes.ERR_EVIDENCE_INVALID);
  }
}
