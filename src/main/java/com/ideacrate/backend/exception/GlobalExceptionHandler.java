package com.ideacrate.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized error handling for all @RestController-annotated classes.
 *
 * Without this, unhandled exceptions bubble up as raw HTTP 500 responses that
 * expose internal stack trace messages to the client. This handler intercepts
 * specific exception types and returns clean, structured JSON error bodies.
 *
 * Standard error response shape:
 * {
 *   "timestamp": "2026-03-14T22:00:00",
 *   "status":    400,
 *   "error":     "Bad Request",
 *   "message":   "User with email x@y.com already exists.",
 *   "path":      "/api/v1/auth/register"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------------
    // Business / validation errors (registration duplicate, bad password, etc.)
    // These are thrown from UserService and ProjectService as
    // IllegalStateException or IllegalArgumentException.
    // ------------------------------------------------------------------

    /**
     * Handles bad-input errors from the service layer (e.g. duplicate email,
     * invalid login, null project request). Returns HTTP 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        logger.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Handles business rule violations from the service layer (e.g. user
     * already exists, invalid credentials). Returns HTTP 409 Conflict for
     * duplicate-entity situations and 400 for credential errors.
     *
     * We use 409 here because "already exists" is semantically a conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        logger.warn("Business rule violation: {}", ex.getMessage());

        // Use 409 for "already exists" scenarios, 401 for auth failures.
        HttpStatus status = ex.getMessage().toLowerCase().contains("already exists")
                ? HttpStatus.CONFLICT
                : HttpStatus.UNAUTHORIZED;

        return buildErrorResponse(status, ex.getMessage(), request);
    }

    /**
     * Handles resource-not-found errors thrown from the service layer as
     * RuntimeException (e.g. "Project Not Found"). Returns HTTP 404.
     *
     * Ideally these would be a custom ResourceNotFoundException, but since the
     * codebase currently uses RuntimeException directly, we handle it here and
     * detect "not found" messages by convention.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        logger.error("Runtime error: {}", ex.getMessage());

        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();

        // Route to 404 if the message is about a missing resource
        if (msg.contains("not found")) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        }

        // Route to 403 if the message is about authorization (e.g. "Only project owner can...")
        if (msg.contains("only") || msg.contains("owner") || msg.contains("forbidden")) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
        }

        // Route to 409 if the message is about a duplicate (e.g. "already a contributor")
        if (msg.contains("already")) {
            return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
        }

        // Default to 500 for truly unexpected runtime errors
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.", request);
    }

    /**
     * Catch-all handler for any exception not matched by the more specific handlers above.
     * Always returns HTTP 500 and avoids leaking internal details to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        // Log at ERROR level with a full stack trace so we can diagnose in logs
        logger.error("Unhandled exception at {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal server error occurred. Please contact support.", request);
    }

    // ------------------------------------------------------------------
    // Private helper
    // ------------------------------------------------------------------

    /**
     * Builds the standard error response map.
     *
     * @param status  HTTP status to return
     * @param message Human-readable error message
     * @param request The current web request (used to extract the path)
     * @return A ResponseEntity wrapping the error map
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message != null ? message : "No message available");
        // Strip "uri=" prefix that WebRequest.getDescription adds
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, status);
    }
}
