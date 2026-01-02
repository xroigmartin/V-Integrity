package xavierroigmartin.v_integrity.interfaces.rest.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import xavierroigmartin.v_integrity.application.exception.ApplicationException;
import xavierroigmartin.v_integrity.domain.exception.DomainException;
import xavierroigmartin.v_integrity.infrastructure.exception.InfrastructureException;

/**
 * Global exception handler for the REST API.
 * <p>
 * Captures exceptions thrown by controllers and transforms them into standard RFC 7807 Problem Details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles validation errors (@Valid).
   * Returns a ProblemDetail with validation messages.
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    String errors = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getField)
        .collect(Collectors.joining(", "));

    String message = "Validation failed for fields: " + errors;
    logger.warn("Validation Error: {}", message);

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
    problem.setTitle("Validation Failed");
    problem.setProperty("timestamp", LocalDateTime.now());

    return createResponseEntity(problem, headers, status, request);
  }

  /**
   * Handles Domain Exceptions (Business Rules).
   * Mapped to 400 Bad Request.
   */
  @ExceptionHandler(DomainException.class)
  public ProblemDetail handleDomainException(DomainException ex) {
    logger.warn("Domain Error: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /**
   * Handles Application Exceptions (Flow/State).
   * Mapped to 400 Bad Request.
   */
  @ExceptionHandler(ApplicationException.class)
  public ProblemDetail handleApplicationException(ApplicationException ex) {
    logger.warn("Application Error: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /**
   * Handles Infrastructure Exceptions (Technical Failures).
   * Mapped to 500 Internal Server Error.
   */
  @ExceptionHandler(InfrastructureException.class)
  public ProblemDetail handleInfrastructureException(InfrastructureException ex) {
    logger.error("Infrastructure Error: {}", ex.getMessage(), ex);
    return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
  }

  /**
   * Handles IllegalArgumentException (Legacy/Standard validations).
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
    logger.warn("Bad Request: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /**
   * Handles IllegalStateException (Legacy/Standard state checks).
   */
  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
    logger.warn("Invalid State: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /**
   * Handles all uncaught exceptions (fallback).
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleAllUncaughtException(Exception ex, HttpServletRequest request) {
    logger.error("Uncaught exception processing request: {}", request.getRequestURI(), ex);
    return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
  }

  private ProblemDetail buildProblemDetail(HttpStatus status, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setProperty("timestamp", LocalDateTime.now());
    return problem;
  }
}
