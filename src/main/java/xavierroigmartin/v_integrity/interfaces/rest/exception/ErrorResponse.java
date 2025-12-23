package xavierroigmartin.v_integrity.interfaces.rest.exception;

import java.time.LocalDateTime;

/**
 * Standard error response structure for the REST API.
 *
 * @param timestamp When the error occurred.
 * @param status    HTTP status code.
 * @param error     HTTP error reason phrase.
 * @param message   Detailed error message.
 * @param path      Request URI that caused the error.
 */
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
) {}
