package com.crowdshield.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionRequest {
    
    @jakarta.validation.constraints.NotNull(message = "content_id is required")
    private UUID contentId;
    
    @NotBlank(message = "new_label is required")
    @Pattern(regexp = "^(SAFE|FLAGGED)$", message = "new_label must be SAFE or FLAGGED")
    private String newLabel;
    
    private String note;
}

