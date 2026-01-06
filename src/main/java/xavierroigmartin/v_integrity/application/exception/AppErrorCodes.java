package xavierroigmartin.v_integrity.application.exception;

/**
 * Standard application error codes for API responses.
 */
public final class AppErrorCodes {

  private AppErrorCodes() {}

  // General
  public static final String ERR_VALIDATION = "ERR_VALIDATION";
  public static final String ERR_UNEXPECTED = "ERR_UNEXPECTED";
  public static final String ERR_INFRASTRUCTURE = "ERR_INFRASTRUCTURE";

  // Domain
  public static final String ERR_DOMAIN_RULE = "ERR_DOMAIN_RULE";
  public static final String ERR_BLOCK_INVALID = "ERR_BLOCK_INVALID";
  public static final String ERR_EVIDENCE_INVALID = "ERR_EVIDENCE_INVALID";

  // Application
  public static final String ERR_APPLICATION_STATE = "ERR_APPLICATION_STATE";
  public static final String ERR_NOT_LEADER = "ERR_NOT_LEADER";
  public static final String ERR_MEMPOOL_EMPTY = "ERR_MEMPOOL_EMPTY";
}
