package com.crowdshield.service;

import com.crowdshield.model.AdminAction;
import com.crowdshield.model.Content;
import com.crowdshield.repository.AdminActionRepository;
import com.crowdshield.repository.ContentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AdminService {

    private final ContentRepository contentRepository;
    private final AdminActionRepository adminActionRepository;

    public AdminService(
            ContentRepository contentRepository,
            AdminActionRepository adminActionRepository) {
        this.contentRepository = contentRepository;
        this.adminActionRepository = adminActionRepository;
    }

    // Retrieves all content items that have been flagged as inappropriate
    public List<Content> getFlaggedContent() {
        return contentRepository.findFlaggedContent();
    }

    // Allows admin to manually override moderation decision and records the action
    @Transactional
    public AdminAction overrideDecision(UUID contentId, String adminId, String newLabel, String note) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found: " + contentId));

        String previousLabel = content.getStatus() == Content.ContentStatus.FLAGGED ? "FLAGGED" : "SAFE";
        
        Content.ContentStatus newStatus = "FLAGGED".equals(newLabel) 
                ? Content.ContentStatus.FLAGGED 
                : Content.ContentStatus.SAFE;

        content.setStatus(newStatus);
        contentRepository.save(content);

        AdminAction action = AdminAction.builder()
                .contentId(contentId)
                .adminId(adminId)
                .previousLabel(previousLabel)
                .newLabel(newLabel)
                .note(note)
                .build();

        AdminAction saved = adminActionRepository.save(action);
        
        log.info("Admin override - content_id: {}, admin_id: {}, previous: {}, new: {}", 
                contentId, adminId, previousLabel, newLabel);

        return saved;
    }

    // Retrieves complete moderation history including admin overrides for a content item
    public List<AdminAction> getModerationHistory(UUID contentId) {
        return adminActionRepository.findByContentIdOrderByCreatedAtDesc(contentId);
    }

    // Deletes a content item and all associated data (moderation results, admin actions, jobs)
    @Transactional
    public void deleteContent(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found: " + contentId));
        
        // Delete content (cascade will handle related records)
        contentRepository.delete(content);
        
        log.info("Admin deleted content - content_id: {}", contentId);
    }
}

