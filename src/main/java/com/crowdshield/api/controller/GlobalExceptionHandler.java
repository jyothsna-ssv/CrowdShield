package com.crowdshield.api.controller;

import com.crowdshield.api.dto.ErrorResponse;
import com.crowdshield.util.ErrorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles validation errors from request body validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String message = errors.values().stream().findFirst().orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(ErrorUtils.createErrorResponse("VALIDATION_ERROR", message));
    }

    // Handles runtime exceptions and returns 500 error response
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorUtils.createErrorResponse("RUNTIME_ERROR", ex.getMessage()));
    }

    // Handles database and Redis access exceptions
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(org.springframework.dao.DataAccessException ex) {
        log.error("Database/Redis access exception occurred", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "Data access error occurred";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorUtils.createErrorResponse("DATA_ACCESS_ERROR", message));
    }

    // Handles 404 errors when endpoint is not found
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        String path = ex.getRequestURL();
        String message = String.format("Endpoint not found: %s. Available endpoints: /api/content, /api/admin, /api/rules, /actuator/health", path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorUtils.createErrorResponse("NOT_FOUND", message));
    }

    // Handles 404 errors when static resource is not found
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        String path = ex.getResourcePath();
        String message = String.format("Resource not found: %s. This is an API service. Available endpoints: /api/content, /api/admin, /api/rules, /actuator/health", path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorUtils.createErrorResponse("NOT_FOUND", message));
    }

    // Handles all other unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred: {}", ex.getClass().getName(), ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", message));
    }
}

