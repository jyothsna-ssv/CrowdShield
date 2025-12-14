package com.crowdshield.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationScores {
    
    private Float toxicityScore;
    private Float hateScore;
    private Float sexualScore;
    private Float violenceScore;
    private Map<String, Object> rawResponse;
}

