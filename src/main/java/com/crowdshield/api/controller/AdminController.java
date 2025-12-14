package com.crowdshield.api.controller;

import com.crowdshield.api.dto.*;
import com.crowdshield.model.AdminAction;
import com.crowdshield.model.Content;
import com.crowdshield.model.ModerationResult;
import com.crowdshield.repository.ContentRepository;
import com.crowdshield.service.AdminService;
import com.crowdshield.service.ContentService;
import com.crowdshield.service.ModerationService;
import com.crowdshield.util.ErrorUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ModerationService moderationService;
    private final ContentService contentService;
    private final ContentRepository contentRepository;

    public AdminController(AdminService adminService, ModerationService moderationService, ContentService contentService, ContentRepository contentRepository) {
        this.adminService = adminService;
        this.moderationService = moderationService;
        this.contentService = contentService;
        this.contentRepository = contentRepository;
    }

    @GetMapping("/flagged")
    public ResponseEntity<?> getFlaggedContent(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT validation is handled by filter, but we can add additional checks here if needed
        try {
            List<Content> flaggedContent = adminService.getFlaggedContent();

            List<FlaggedContentResponse> responses = flaggedContent.stream()
                    .map(content -> {
                        Optional<ModerationResult> resultOpt = moderationService.getModerationResult(content.getId());
                        
                        ContentResponse.Scores scores = null;
                        if (resultOpt.isPresent()) {
                            ModerationResult result = resultOpt.get();
                            scores = ContentResponse.Scores.builder()
                                    .toxicity(result.getToxicityScore())
                                    .hate(result.getHateScore())
                                    .sexual(result.getSexualScore())
                                    .violence(result.getViolenceScore())
                                    .build();
                        }

                        String preview = content.getType() == Content.ContentType.TEXT 
                                ? (content.getTextContent() != null && content.getTextContent().length() > 100 
                                        ? content.getTextContent().substring(0, 100) + "..." 
                                        : content.getTextContent())
                                : content.getImageUrl();

                        return FlaggedContentResponse.builder()
                                .contentId(content.getId())
                                .userId(content.getUserId())
                                .type(content.getType().name())
                                .preview(preview)
                                .status(content.getStatus().name())
                                .scores(scores)
                                .createdAt(content.getCreatedAt())
                                .build();
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("Error retrieving flagged content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    // Allows admin to manually override moderation decision for a content item
    @PostMapping("/action")
    public ResponseEntity<?> overrideDecision(
            @Valid @RequestBody AdminActionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT validation is handled by filter
        try {
            // Get adminId from JWT token (set by JwtAuthenticationFilter)
            String adminId = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getName()
                    : "unknown";
            
            AdminAction action = adminService.overrideDecision(
                    request.getContentId(),
                    adminId,
                    request.getNewLabel(),
                    request.getNote()
            );

            AdminActionResponse response = AdminActionResponse.builder()
                    .success(true)
                    .previousLabel(action.getPreviousLabel())
                    .newLabel(action.getNewLabel())
                    .build();

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error overriding decision", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorUtils.createErrorResponse("CONTENT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error overriding decision", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    // Retrieves complete moderation history including initial decision and admin overrides
    @GetMapping("/history/{contentId}")
    public ResponseEntity<?> getModerationHistory(
            @PathVariable UUID contentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT validation is handled by filter
        try {
            Optional<Content> contentOpt = contentService.getContent(contentId);

            if (contentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorUtils.createErrorResponse("CONTENT_NOT_FOUND", 
                                "Content with id " + contentId + " not found"));
            }

            Optional<ModerationResult> resultOpt = moderationService.getModerationResult(contentId);
            List<AdminAction> adminActions = adminService.getModerationHistory(contentId);

            ModerationHistoryResponse.InitialDecision initialDecision = null;
            if (resultOpt.isPresent()) {
                ModerationResult result = resultOpt.get();
                initialDecision = ModerationHistoryResponse.InitialDecision.builder()
                        .label(result.getOverallLabel() != null ? result.getOverallLabel().name() : "UNKNOWN")
                        .scores(ContentResponse.Scores.builder()
                                .toxicity(result.getToxicityScore())
                                .hate(result.getHateScore())
                                .sexual(result.getSexualScore())
                                .violence(result.getViolenceScore())
                                .build())
                        .timestamp(result.getCreatedAt())
                        .build();
            }

            List<ModerationHistoryResponse.AdminOverride> overrides = adminActions.stream()
                    .map(action -> ModerationHistoryResponse.AdminOverride.builder()
                            .adminId(action.getAdminId())
                            .previousLabel(action.getPreviousLabel())
                            .newLabel(action.getNewLabel())
                            .note(action.getNote())
                            .timestamp(action.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            ModerationHistoryResponse response = ModerationHistoryResponse.builder()
                    .contentId(contentId)
                    .initialDecision(initialDecision)
                    .adminOverrides(overrides)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving moderation history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    // Deletes a content item and all associated data
    @DeleteMapping("/content/{contentId}")
    public ResponseEntity<?> deleteContent(
            @PathVariable UUID contentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT validation is handled by filter
        try {
            adminService.deleteContent(contentId);
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Content deleted successfully"
            ));
        } catch (RuntimeException e) {
            log.error("Error deleting content", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorUtils.createErrorResponse("CONTENT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    // Fixes content items stuck in PROCESSING status by checking for results or marking as ERROR
    @PostMapping("/fix-stuck-processing")
    public ResponseEntity<?> fixStuckProcessing(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Find all content stuck in PROCESSING status for more than 5 minutes
            List<Content> stuckContent = contentRepository.findAll().stream()
                    .filter(c -> c.getStatus() == Content.ContentStatus.PROCESSING)
                    .filter(c -> c.getUpdatedAt() != null && 
                            c.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(5)))
                    .collect(Collectors.toList());

            int fixed = 0;
            for (Content content : stuckContent) {
                // Check if there's a moderation result (job completed but status not updated)
                Optional<ModerationResult> resultOpt = moderationService.getModerationResult(content.getId());
                if (resultOpt.isPresent()) {
                    ModerationResult result = resultOpt.get();
                    Content.ContentStatus newStatus = result.getOverallLabel() == ModerationResult.ModerationLabel.FLAGGED
                            ? Content.ContentStatus.FLAGGED
                            : Content.ContentStatus.SAFE;
                    content.setStatus(newStatus);
                    contentRepository.save(content);
                    fixed++;
                    log.info("Fixed stuck content {} - updated to {}", content.getId(), newStatus);
                } else {
                    // No result means job failed - mark as ERROR
                    content.setStatus(Content.ContentStatus.ERROR);
                    contentRepository.save(content);
                    fixed++;
                    log.info("Fixed stuck content {} - marked as ERROR (no result)", content.getId());
                }
            }

            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Fixed stuck processing items",
                    "count", fixed
            ));
        } catch (Exception e) {
            log.error("Error fixing stuck processing items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }
}
