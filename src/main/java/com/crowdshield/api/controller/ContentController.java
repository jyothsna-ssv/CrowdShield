package com.crowdshield.api.controller;

import com.crowdshield.api.dto.ContentRequest;
import com.crowdshield.api.dto.ContentResponse;
import com.crowdshield.api.dto.ImageRequest;
import com.crowdshield.model.Content;
import com.crowdshield.model.ModerationResult;
import com.crowdshield.service.ContentService;
import com.crowdshield.service.ModerationService;
import com.crowdshield.service.QueueService;
import com.crowdshield.util.ErrorUtils;
import com.crowdshield.util.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;
    private final QueueService queueService;
    private final ModerationService moderationService;
    private final RateLimiter rateLimiter;
    private final com.crowdshield.service.WebSocketService webSocketService;

    public ContentController(
            ContentService contentService,
            QueueService queueService,
            ModerationService moderationService,
            RateLimiter rateLimiter,
            com.crowdshield.service.WebSocketService webSocketService) {
        this.contentService = contentService;
        this.queueService = queueService;
        this.moderationService = moderationService;
        this.rateLimiter = rateLimiter;
        this.webSocketService = webSocketService;
    }

    // Handles text content submission, validates input, applies rate limiting, creates content, and queues for moderation
    @PostMapping("/text")
    public ResponseEntity<?> submitText(@RequestBody ContentRequest request) {
        try {
            // Use anonymous if user_id not provided
            String userId = (request.getUserId() != null && !request.getUserId().trim().isEmpty()) 
                    ? request.getUserId() 
                    : "anonymous_" + System.currentTimeMillis();

            // Rate limiting
            if (!rateLimiter.allowRequest(userId)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ErrorUtils.createErrorResponse("RATE_LIMIT_EXCEEDED", 
                                "Too many requests. Please try again later."));
            }

            // Validate text
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ErrorUtils.createErrorResponse("INVALID_TEXT", "Text cannot be empty"));
            }

            // Create content
            Content content = contentService.createTextContent(userId, request.getText());

            // Send initial PENDING status via WebSocket (10%)
            webSocketService.sendProgressUpdate(content.getId(), "PENDING", 10);

            // Create and push job to queue
            UUID jobId = UUID.randomUUID();
            queueService.pushToMainQueue(
                    jobId,
                    content.getId(),
                    "TEXT",
                    request.getText(),
                    null,
                    0
            );

            ContentResponse response = ContentResponse.builder()
                    .contentId(content.getId())
                    .status(content.getStatus().name())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error submitting text content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    // Handles image content submission, validates URL, applies rate limiting, creates content, and queues for moderation
    @PostMapping("/image")
    public ResponseEntity<?> submitImage(@RequestBody ImageRequest request) {
        try {
            // Use anonymous if user_id not provided
            String userId = (request.getUserId() != null && !request.getUserId().trim().isEmpty()) 
                    ? request.getUserId() 
                    : "anonymous_" + System.currentTimeMillis();

            // Rate limiting
            if (!rateLimiter.allowRequest(userId)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ErrorUtils.createErrorResponse("RATE_LIMIT_EXCEEDED", 
                                "Too many requests. Please try again later."));
            }

            // Validate image URL
            if (request.getImageUrl() == null || request.getImageUrl().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ErrorUtils.createErrorResponse("INVALID_IMAGE_URL", "Image URL cannot be empty"));
            }

            // Create content
            Content content = contentService.createImageContent(userId, request.getImageUrl());

            // Send initial PENDING status via WebSocket (10%)
            webSocketService.sendProgressUpdate(content.getId(), "PENDING", 10);

            // Create and push job to queue
            UUID jobId = UUID.randomUUID();
            queueService.pushToMainQueue(
                    jobId,
                    content.getId(),
                    "IMAGE",
                    null,
                    request.getImageUrl(),
                    0
            );

            ContentResponse response = ContentResponse.builder()
                    .contentId(content.getId())
                    .status(content.getStatus().name())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error submitting image content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    // Retrieves content status and moderation results for a given content ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getContentStatus(@PathVariable UUID id) {
        try {
            Optional<Content> contentOpt = contentService.getContent(id);
            
            if (contentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorUtils.createErrorResponse("CONTENT_NOT_FOUND", 
                                "Content with id " + id + " not found"));
            }

            Content content = contentOpt.get();
            Optional<ModerationResult> resultOpt = moderationService.getModerationResult(id);

            ContentResponse.Scores scores = null;
            String label = null;

            if (resultOpt.isPresent()) {
                ModerationResult result = resultOpt.get();
                scores = ContentResponse.Scores.builder()
                        .toxicity(result.getToxicityScore())
                        .hate(result.getHateScore())
                        .sexual(result.getSexualScore())
                        .violence(result.getViolenceScore())
                        .build();
                label = result.getOverallLabel() != null ? result.getOverallLabel().name() : null;
            }

            ContentResponse response = ContentResponse.builder()
                    .contentId(content.getId())
                    .status(content.getStatus().name())
                    .scores(scores)
                    .label(label)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving content status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }
}

