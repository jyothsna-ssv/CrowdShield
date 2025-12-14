package com.crowdshield.api.controller;

import com.crowdshield.api.dto.RuleResponse;
import com.crowdshield.api.dto.RuleUpdateRequest;
import com.crowdshield.model.ModerationRule;
import com.crowdshield.service.RuleEngineService;
import com.crowdshield.util.ErrorUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rules")
public class RulesController {

    private final RuleEngineService ruleEngineService;

    public RulesController(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }

    // Retrieves current moderation rule thresholds
    @GetMapping
    public ResponseEntity<?> getRules() {
        try {
            ModerationRule rule = ruleEngineService.getLatestRule();

            RuleResponse response = RuleResponse.builder()
                    .toxicityThreshold(rule.getToxicityThreshold())
                    .hateThreshold(rule.getHateThreshold())
                    .sexualThreshold(rule.getSexualThreshold())
                    .violenceThreshold(rule.getViolenceThreshold())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving rules", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    // Updates moderation rule thresholds with new values
    @PostMapping
    public ResponseEntity<?> updateRules(@Valid @RequestBody RuleUpdateRequest request) {
        try {
            ModerationRule updated = ruleEngineService.updateRule(
                    request.getToxicityThreshold(),
                    request.getHateThreshold(),
                    request.getSexualThreshold(),
                    request.getViolenceThreshold()
            );

            RuleResponse response = RuleResponse.builder()
                    .toxicityThreshold(updated.getToxicityThreshold())
                    .hateThreshold(updated.getHateThreshold())
                    .sexualThreshold(updated.getSexualThreshold())
                    .violenceThreshold(updated.getViolenceThreshold())
                    .build();

            return ResponseEntity.ok(Map.of("updated", true, "rules", response));

        } catch (Exception e) {
            log.error("Error updating rules", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorUtils.createErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }
}

