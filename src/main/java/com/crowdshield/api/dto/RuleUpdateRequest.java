package com.crowdshield.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleUpdateRequest {
    
    @DecimalMin(value = "0.0", message = "toxicity_threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "toxicity_threshold must be between 0.0 and 1.0")
    private Float toxicityThreshold;
    
    @DecimalMin(value = "0.0", message = "hate_threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "hate_threshold must be between 0.0 and 1.0")
    private Float hateThreshold;
    
    @DecimalMin(value = "0.0", message = "sexual_threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "sexual_threshold must be between 0.0 and 1.0")
    private Float sexualThreshold;
    
    @DecimalMin(value = "0.0", message = "violence_threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "violence_threshold must be between 0.0 and 1.0")
    private Float violenceThreshold;
}

