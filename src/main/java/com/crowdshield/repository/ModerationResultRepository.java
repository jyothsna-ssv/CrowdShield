package com.crowdshield.repository;

import com.crowdshield.model.ModerationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModerationResultRepository extends JpaRepository<ModerationResult, UUID> {
    
    Optional<ModerationResult> findByContentId(UUID contentId);
}

