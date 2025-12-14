package com.crowdshield.repository;

import com.crowdshield.model.ModerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModerationJobRepository extends JpaRepository<ModerationJob, UUID> {
    
    Optional<ModerationJob> findByContentId(UUID contentId);
}

