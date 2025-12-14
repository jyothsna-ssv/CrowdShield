package com.crowdshield.api.controller;

import com.crowdshield.api.dto.ContentResponse;
import com.crowdshield.model.Content;
import com.crowdshield.repository.ContentRepository;
import com.crowdshield.repository.ModerationResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final ContentRepository contentRepository;
    private final ModerationResultRepository moderationResultRepository;
    private final com.crowdshield.util.JwtUtil jwtUtil;

    public AdminDashboardController(
            ContentRepository contentRepository,
            ModerationResultRepository moderationResultRepository,
            com.crowdshield.util.JwtUtil jwtUtil) {
        this.contentRepository = contentRepository;
        this.moderationResultRepository = moderationResultRepository;
        this.jwtUtil = jwtUtil;
    }

    // Validates JWT token from Authorization header to check if user is admin
    private boolean isAdmin(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.validateToken(token);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    // Retrieves paginated list of all content with optional status filter
    @GetMapping("/content")
    public ResponseEntity<?> getAllContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Content> contentPage;

        if (status != null && !status.isEmpty()) {
            try {
                Content.ContentStatus contentStatus = Content.ContentStatus.valueOf(status.toUpperCase());
                contentPage = contentRepository.findByStatus(contentStatus, pageable);
            } catch (IllegalArgumentException e) {
                contentPage = contentRepository.findAll(pageable);
            }
        } else {
            contentPage = contentRepository.findAll(pageable);
        }

        List<ContentResponse> contentList = contentPage.getContent().stream()
                .map((Content content) -> {
                    var result = moderationResultRepository.findByContentId(content.getId());
                    String preview = "";
                    if (content.getType() == Content.ContentType.TEXT) {
                        String text = content.getTextContent() != null ? content.getTextContent() : "";
                        preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                    } else {
                        preview = content.getImageUrl() != null ? content.getImageUrl() : "";
                    }
                    
                    ContentResponse.Scores scores = null;
                    String label = null;
                    if (result.isPresent()) {
                        var r = result.get();
                        scores = new ContentResponse.Scores(
                                r.getToxicityScore(),
                                r.getHateScore(),
                                r.getSexualScore(),
                                r.getViolenceScore()
                        );
                        label = r.getOverallLabel().name();
                    }
                    
                    return ContentResponse.builder()
                            .contentId(content.getId())
                            .status(content.getStatus().name())
                            .userId(content.getUserId())
                            .contentType(content.getType().name())
                            .preview(preview)
                            .createdAt(content.getCreatedAt())
                            .scores(scores)
                            .label(label)
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", contentList);
        response.put("totalElements", contentPage.getTotalElements());
        response.put("totalPages", contentPage.getTotalPages());
        response.put("currentPage", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }

    // Retrieves dashboard statistics including counts by status
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        long totalContent = contentRepository.count();
        long pendingContent = contentRepository.countByStatus(Content.ContentStatus.PENDING);
        long processingContent = contentRepository.countByStatus(Content.ContentStatus.PROCESSING);
        long safeContent = contentRepository.countByStatus(Content.ContentStatus.SAFE);
        long flaggedContent = contentRepository.countByStatus(Content.ContentStatus.FLAGGED);
        long errorContent = contentRepository.countByStatus(Content.ContentStatus.ERROR);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalContent);
        stats.put("pending", pendingContent);
        stats.put("processing", processingContent);
        stats.put("safe", safeContent);
        stats.put("flagged", flaggedContent);
        stats.put("error", errorContent);

        return ResponseEntity.ok(stats);
    }
}

