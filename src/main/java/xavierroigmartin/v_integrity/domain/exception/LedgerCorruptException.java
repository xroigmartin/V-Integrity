package xavierroigmartin.v_integrity.domain.exception;

import xavierroigmartin.v_integrity.application.exception.AppErrorCodes;

/**
 * Thrown when the ledger integrity check fails (e.g. invalid hash, broken chain link, bad signature).
 * This is a critical domain error indicating data corruption.
 */
public class LedgerCorruptException extends DomainException {
  public LedgerCorruptException(String message) {
    super(message, AppErrorCodes.ERR_BLOCK_INVALID); // Reusing block invalid code or define a new critical one
  }
}
