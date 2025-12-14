package com.crowdshield.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequest {
    
    @JsonProperty("user_id")
    private String userId; // Optional - will use anonymous if not provided
    
    @NotBlank(message = "image_url is required")
    @Pattern(regexp = "^https?://.*", message = "image_url must be a valid URL")
    @JsonProperty("image_url")
    private String imageUrl;
}

