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
public class FlaggedContentResponse {
    
    private UUID contentId;
    private String userId;
    private String type;
    private String preview;
    private String status;
    private ContentResponse.Scores scores;
    private LocalDateTime createdAt;
}

