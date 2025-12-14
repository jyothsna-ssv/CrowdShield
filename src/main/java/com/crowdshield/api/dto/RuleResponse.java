package com.crowdshield.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResponse {
    
    private Float toxicityThreshold;
    private Float hateThreshold;
    private Float sexualThreshold;
    private Float violenceThreshold;
}

