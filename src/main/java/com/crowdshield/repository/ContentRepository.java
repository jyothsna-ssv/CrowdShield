package com.crowdshield.repository;

import com.crowdshield.model.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {
    
    List<Content> findByStatus(Content.ContentStatus status);
    
    Page<Content> findByStatus(Content.ContentStatus status, Pageable pageable);
    
    long countByStatus(Content.ContentStatus status);
    
    @Query("SELECT c FROM Content c WHERE c.status = 'FLAGGED' ORDER BY c.createdAt DESC")
    List<Content> findFlaggedContent();
}

