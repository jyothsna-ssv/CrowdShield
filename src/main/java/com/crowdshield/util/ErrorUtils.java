package com.crowdshield.util;

import com.crowdshield.api.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrorUtils {
    
    // Creates an error response DTO with error code and message
    public static ErrorResponse createErrorResponse(String error, String message) {
        log.error("Error: {} - Message: {}", error, message);
        return ErrorResponse.of(error, message);
    }
    
    // Creates an error response DTO from exception, extracting message or using default
    public static ErrorResponse createErrorResponse(String error, Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "Internal server error";
        log.error("Error: {} - Exception: {}", error, e.getMessage(), e);
        return ErrorResponse.of(error, message);
    }
}

