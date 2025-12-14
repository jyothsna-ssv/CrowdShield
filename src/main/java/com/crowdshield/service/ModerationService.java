package com.crowdshield.service;

import com.crowdshield.api.dto.ModerationScores;
import com.crowdshield.model.Content;
import com.crowdshield.model.ModerationResult;
import com.crowdshield.repository.ModerationResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ModerationService {

    private final ModerationResultRepository resultRepository;
    private final RuleEngineService ruleEngineService;
    private final ContentService contentService;
    private final WebSocketService webSocketService;

    public ModerationService(
            ModerationResultRepository resultRepository,
            RuleEngineService ruleEngineService,
            ContentService contentService,
            WebSocketService webSocketService) {
        this.resultRepository = resultRepository;
        this.ruleEngineService = ruleEngineService;
        this.contentService = contentService;
        this.webSocketService = webSocketService;
    }

    // Saves moderation result, evaluates scores using rule engine, updates content status, and sends WebSocket notification
    @Transactional
    public ModerationResult saveModerationResult(UUID contentId, ModerationScores scores) {
        ModerationResult.ModerationLabel label = ruleEngineService.evaluate(scores);

        ModerationResult result = ModerationResult.builder()
                .contentId(contentId)
                .toxicityScore(scores.getToxicityScore())
                .hateScore(scores.getHateScore())
                .sexualScore(scores.getSexualScore())
                .violenceScore(scores.getViolenceScore())
                .overallLabel(label)
                .rawResponse(scores.getRawResponse())
                .build();

        ModerationResult saved = resultRepository.save(result);

        // Update content status
        Content.ContentStatus status = label == ModerationResult.ModerationLabel.FLAGGED 
                ? Content.ContentStatus.FLAGGED 
                : Content.ContentStatus.SAFE;
        contentService.updateStatus(contentId, status);

        // Send final WebSocket update
        String finalStatus = status == Content.ContentStatus.FLAGGED ? "FLAGGED" : "SAFE";
        webSocketService.sendStatusUpdate(contentId, "DONE", finalStatus);
        webSocketService.sendProgressUpdate(contentId, "DONE", 100);

        log.info("Saved moderation result - content_id: {}, label: {}", contentId, label);

        return saved;
    }

    // Retrieves moderation result for a given content ID
    public Optional<ModerationResult> getModerationResult(UUID contentId) {
        return resultRepository.findByContentId(contentId);
    }
}

