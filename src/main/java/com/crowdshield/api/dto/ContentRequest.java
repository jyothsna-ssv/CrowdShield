package com.crowdshield.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRequest {
    
    @JsonProperty("user_id")
    private String userId; // Optional - will use anonymous if not provided
    
    @NotBlank(message = "text cannot be empty")
    private String text;
}

