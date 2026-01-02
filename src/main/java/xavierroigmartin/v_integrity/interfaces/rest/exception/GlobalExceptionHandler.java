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

/**
 * Global exception handler for the REST API.
 * <p>
 * Captures exceptions thrown by controllers and transforms them into a standard {@link ErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles IllegalArgumentException.
   * Usually thrown when arguments are invalid or validation fails.
   *
   * @param ex      The exception thrown.
   * @param request The HTTP request that triggered the exception.
   * @return A ResponseEntity containing the ErrorResponse and HTTP 400 status.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex,
      HttpServletRequest request) {

    logger.warn("Bad Request: {}", ex.getMessage());

    ErrorResponse errorResponse = new ErrorResponse(
        LocalDateTime.now(),
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        ex.getMessage(),
        request.getRequestURI()
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles IllegalStateException.
   * Usually thrown when the invoked method is not appropriate for the current state.
   *
   * @param ex      The exception thrown.
   * @param request The HTTP request that triggered the exception.
   * @return A ResponseEntity containing the ErrorResponse and HTTP 400 status.
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(
      IllegalStateException ex,
      HttpServletRequest request) {

    logger.warn("Invalid State: {}", ex.getMessage());

    ErrorResponse errorResponse = new ErrorResponse(
        LocalDateTime.now(),
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        ex.getMessage(),
        request.getRequestURI()
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles all uncaught exceptions (fallback).
   *
   * @param ex      The exception thrown.
   * @param request The HTTP request that triggered the exception.
   * @return A ResponseEntity containing the ErrorResponse and HTTP 500 status.
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
}
