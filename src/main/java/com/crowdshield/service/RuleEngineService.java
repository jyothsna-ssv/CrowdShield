package com.crowdshield.service;

import com.crowdshield.api.dto.ModerationScores;
import com.crowdshield.model.ModerationResult;
import com.crowdshield.model.ModerationRule;
import com.crowdshield.repository.ModerationRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class RuleEngineService {

    private final ModerationRuleRepository ruleRepository;

    public RuleEngineService(ModerationRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    // Evaluates moderation scores against configured thresholds and returns SAFE or FLAGGED label
    public ModerationResult.ModerationLabel evaluate(ModerationScores scores) {
        ModerationRule rule = getLatestRule();
        
        boolean isFlagged = scores.getToxicityScore() > rule.getToxicityThreshold()
                || scores.getHateScore() > rule.getHateThreshold()
                || scores.getSexualScore() > rule.getSexualThreshold()
                || scores.getViolenceScore() > rule.getViolenceThreshold();

        ModerationResult.ModerationLabel label = isFlagged 
                ? ModerationResult.ModerationLabel.FLAGGED 
                : ModerationResult.ModerationLabel.SAFE;

        log.info("Rule engine evaluation - toxicity: {}, hate: {}, sexual: {}, violence: {}, label: {}", 
                scores.getToxicityScore(), scores.getHateScore(), 
                scores.getSexualScore(), scores.getViolenceScore(), label);

        return label;
    }

    // Retrieves the most recent moderation rule or returns default thresholds if none exists
    public ModerationRule getLatestRule() {
        Optional<ModerationRule> ruleOpt = ruleRepository.findTopByOrderByUpdatedAtDesc();
        if (ruleOpt.isPresent()) {
            return ruleOpt.get();
        }

        // Return default rule if none exists
        return ModerationRule.builder()
                .toxicityThreshold(0.7f)
                .hateThreshold(0.6f)
                .sexualThreshold(0.6f)
                .violenceThreshold(0.6f)
                .build();
    }

    // Updates moderation rule thresholds with provided values and saves to database
    public ModerationRule updateRule(Float toxicityThreshold, Float hateThreshold, 
                                     Float sexualThreshold, Float violenceThreshold) {
        ModerationRule rule = getLatestRule();
        
        if (toxicityThreshold != null) rule.setToxicityThreshold(toxicityThreshold);
        if (hateThreshold != null) rule.setHateThreshold(hateThreshold);
        if (sexualThreshold != null) rule.setSexualThreshold(sexualThreshold);
        if (violenceThreshold != null) rule.setViolenceThreshold(violenceThreshold);

        return ruleRepository.save(rule);
    }
}

