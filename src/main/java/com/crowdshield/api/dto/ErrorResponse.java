package com.crowdshield.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String error;
    private String message;
    private LocalDateTime timestamp;
    
    // Creates an ErrorResponse instance with error code, message, and current timestamp
    public static ErrorResponse of(String error, String message) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

