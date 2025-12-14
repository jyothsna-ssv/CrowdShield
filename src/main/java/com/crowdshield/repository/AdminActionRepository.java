package com.crowdshield.repository;

import com.crowdshield.model.AdminAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminActionRepository extends JpaRepository<AdminAction, UUID> {
    
    List<AdminAction> findByContentIdOrderByCreatedAtDesc(UUID contentId);
}

