package xavierroigmartin.v_integrity.interfaces.rest.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import xavierroigmartin.v_integrity.application.exception.ApplicationException;
import xavierroigmartin.v_integrity.domain.exception.DomainException;
import xavierroigmartin.v_integrity.infrastructure.exception.InfrastructureException;

/**
 * Global exception handler for the REST API.
 * <p>
 * Captures exceptions thrown by controllers and transforms them into a standard {@link ErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles Domain Exceptions (Business Rules).
   * Mapped to 400 Bad Request.
   */
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomainException(
      DomainException ex,
      HttpServletRequest request) {

    logger.warn("Domain Error: {}", ex.getMessage());
    return buildResponse(ex, HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Handles Application Exceptions (Flow/State).
   * Mapped to 400 Bad Request.
   */
  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ErrorResponse> handleApplicationException(
      ApplicationException ex,
      HttpServletRequest request) {

    logger.warn("Application Error: {}", ex.getMessage());
    return buildResponse(ex, HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Handles Infrastructure Exceptions (Technical Failures).
   * Mapped to 500 Internal Server Error.
   */
  @ExceptionHandler(InfrastructureException.class)
  public ResponseEntity<ErrorResponse> handleInfrastructureException(
      InfrastructureException ex,
      HttpServletRequest request) {

    logger.error("Infrastructure Error: {}", ex.getMessage(), ex);
    return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  /**
   * Handles IllegalArgumentException (Legacy/Standard validations).
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex,
      HttpServletRequest request) {

    logger.warn("Bad Request: {}", ex.getMessage());
    return buildResponse(ex, HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Handles IllegalStateException (Legacy/Standard state checks).
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(
      IllegalStateException ex,
      HttpServletRequest request) {

    logger.warn("Invalid State: {}", ex.getMessage());
    return buildResponse(ex, HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Handles all uncaught exceptions (fallback).
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllUncaughtException(
      Exception ex,
      HttpServletRequest request) {

    logger.error("Uncaught exception processing request: {}", request.getRequestURI(), ex);

    ErrorResponse errorResponse = new ErrorResponse(
        LocalDateTime.now(),
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        ex.getMessage(),
        request.getRequestURI()
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponse> buildResponse(Exception ex, HttpStatus status, HttpServletRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
        LocalDateTime.now(),
        status.value(),
        status.getReasonPhrase(),
        ex.getMessage(),
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, status);
  }
}
