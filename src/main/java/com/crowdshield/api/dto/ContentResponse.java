package com.crowdshield.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {
    
    private UUID contentId;
    private String status;
    private Scores scores;
    private String label;
    private LocalDateTime createdAt;
    private String userId;
    private String contentType;
    private String preview;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scores {
        private Float toxicity;
        private Float hate;
        private Float sexual;
        private Float violence;
    }
}

