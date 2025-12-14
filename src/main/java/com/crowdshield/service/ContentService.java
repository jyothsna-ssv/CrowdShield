package com.crowdshield.service;

import com.crowdshield.model.Content;
import com.crowdshield.repository.ContentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    // Creates and saves a new text content entry with PENDING status
    @Transactional
    public Content createTextContent(String userId, String text) {
        Content content = Content.builder()
                .userId(userId)
                .type(Content.ContentType.TEXT)
                .textContent(text)
                .status(Content.ContentStatus.PENDING)
                .build();

        Content saved = contentRepository.save(content);
        log.info("Created text content - content_id: {}, user_id: {}", saved.getId(), userId);
        
        return saved;
    }

    // Creates and saves a new image content entry with PENDING status
    @Transactional
    public Content createImageContent(String userId, String imageUrl) {
        Content content = Content.builder()
                .userId(userId)
                .type(Content.ContentType.IMAGE)
                .imageUrl(imageUrl)
                .status(Content.ContentStatus.PENDING)
                .build();

        Content saved = contentRepository.save(content);
        log.info("Created image content - content_id: {}, user_id: {}", saved.getId(), userId);
        
        return saved;
    }

    // Retrieves content by ID from the database
    public Optional<Content> getContent(UUID contentId) {
        return contentRepository.findById(contentId);
    }

    // Updates the status of content with the given ID
    @Transactional
    public Content updateStatus(UUID contentId, Content.ContentStatus status) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found: " + contentId));
        
        content.setStatus(status);
        Content updated = contentRepository.save(content);
        
        log.info("Updated content status - content_id: {}, status: {}", contentId, status);
        
        return updated;
    }
}

