package com.crowdshield.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionResponse {
    
    private Boolean success;
    private String previousLabel;
    private String newLabel;
}

