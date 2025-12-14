package com.crowdshield.repository;

import com.crowdshield.model.ModerationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModerationRuleRepository extends JpaRepository<ModerationRule, Integer> {
    
    Optional<ModerationRule> findTopByOrderByUpdatedAtDesc();
}

