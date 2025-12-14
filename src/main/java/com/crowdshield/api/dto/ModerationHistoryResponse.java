package com.crowdshield.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationHistoryResponse {
    
    private UUID contentId;
    private InitialDecision initialDecision;
    private List<AdminOverride> adminOverrides;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitialDecision {
        private String label;
        private ContentResponse.Scores scores;
        private LocalDateTime timestamp;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminOverride {
        private String adminId;
        private String previousLabel;
        private String newLabel;
        private String note;
        private LocalDateTime timestamp;
    }
}

