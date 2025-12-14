package com.crowdshield.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Sends status update message to WebSocket subscribers for the given content ID
    public void sendStatusUpdate(UUID contentId, String stage, String status) {
        try {
            Map<String, Object> message = Map.of(
                    "contentId", contentId.toString(),
                    "stage", stage,
                    "status", status,
                    "timestamp", System.currentTimeMillis()
            );

            String destination = "/topic/status/" + contentId;
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Sent WebSocket update - contentId: {}, stage: {}, status: {}", contentId, stage, status);
        } catch (Exception e) {
            log.error("Failed to send WebSocket update for contentId: {}", contentId, e);
        }
    }

    // Sends progress update with percentage to WebSocket subscribers for the given content ID
    public void sendProgressUpdate(UUID contentId, String stage, int progress) {
        try {
            Map<String, Object> message = Map.of(
                    "contentId", contentId.toString(),
                    "stage", stage,
                    "progress", progress,
                    "timestamp", System.currentTimeMillis()
            );

            String destination = "/topic/status/" + contentId;
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Sent progress update - contentId: {}, stage: {}, progress: {}%", contentId, stage, progress);
        } catch (Exception e) {
            log.error("Failed to send progress update for contentId: {}", contentId, e);
        }
    }

    // Sends error message to WebSocket subscribers for the given content ID
    public void sendError(UUID contentId, String errorMessage) {
        try {
            Map<String, Object> message = Map.of(
                    "contentId", contentId.toString(),
                    "stage", "ERROR",
                    "error", errorMessage,
                    "timestamp", System.currentTimeMillis()
            );

            String destination = "/topic/status/" + contentId;
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Sent error update - contentId: {}, error: {}", contentId, errorMessage);
        } catch (Exception e) {
            log.error("Failed to send error update for contentId: {}", contentId, e);
        }
    }
}

