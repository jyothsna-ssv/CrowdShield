package com.crowdshield.worker;

import com.crowdshield.api.dto.ModerationScores;
import com.crowdshield.client.MLModerationClient;
import com.crowdshield.model.Content;
import com.crowdshield.model.ModerationJob;
import com.crowdshield.queue.RedisKeys;
import com.crowdshield.repository.ContentRepository;
import com.crowdshield.repository.ModerationJobRepository;
import com.crowdshield.service.ModerationService;
import com.crowdshield.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Component
public class ModerationWorker implements CommandLineRunner {

    private final QueueService queueService;
    private final ContentRepository contentRepository;
    private final ModerationService moderationService;
    private final MLModerationClient mlModerationClient;
    private final ModerationJobRepository jobRepository;
    private final com.crowdshield.service.WebSocketService webSocketService;
    private final int maxRetries;

    private volatile boolean running = true;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public ModerationWorker(
            QueueService queueService,
            ContentRepository contentRepository,
            ModerationService moderationService,
            MLModerationClient mlModerationClient,
            ModerationJobRepository jobRepository,
            com.crowdshield.service.WebSocketService webSocketService,
            @Value("${queue.worker.max-retries:3}") int maxRetries) {
        this.queueService = queueService;
        this.contentRepository = contentRepository;
        this.moderationService = moderationService;
        this.mlModerationClient = mlModerationClient;
        this.jobRepository = jobRepository;
        this.webSocketService = webSocketService;
        this.maxRetries = maxRetries;
    }

    // Starts worker threads for main queue and retry queue processing
    @Override
    public void run(String... args) {
        log.info("Starting ModerationWorker...");
        
        // Start worker thread
        executorService.submit(this::processJobs);
        
        // Start retry worker
        executorService.submit(this::processRetryQueue);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down ModerationWorker...");
            running = false;
            executorService.shutdown();
        }));
    }

    private void processJobs() {
        log.info("Worker started - listening to queue: {}", RedisKeys.MAIN_QUEUE);
        
        while (running) {
            try {
                // Use 5 second timeout instead of 0 to avoid Redis connection timeout issues
                // This allows the connection to stay alive while still blocking for jobs
                Map<String, Object> job = queueService.popFromQueue(RedisKeys.MAIN_QUEUE, 5);
                
                if (job != null) {
                    processJob(job);
                }
            } catch (org.springframework.dao.QueryTimeoutException e) {
                // Redis timeout - this is expected when no jobs are available
                // Continue the loop to try again
                log.debug("Redis timeout (no jobs available), continuing...");
            } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
                log.error("Redis connection failure, waiting before retry", e);
                try {
                    Thread.sleep(5000); // Wait 5 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                log.error("Error processing job from main queue", e);
                try {
                    Thread.sleep(1000); // Wait before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // Continuously polls retry queue, applies exponential backoff, and requeues or moves to DLQ
    private void processRetryQueue() {
        log.info("Retry worker started - listening to queue: {}", RedisKeys.RETRY_QUEUE);
        
        while (running) {
            try {
                // Use 5 second timeout instead of 0 to avoid Redis connection timeout issues
                Map<String, Object> job = queueService.popFromQueue(RedisKeys.RETRY_QUEUE, 5);
                
                if (job != null) {
                    int attempts = getIntValue(job, "attempts", 0);
                    
                    if (attempts >= maxRetries) {
                        // Move to DLQ
                        moveToDLQ(job, "Max retries exceeded");
                    } else {
                        // Apply exponential backoff
                        long backoffMs = (long) Math.pow(2, attempts) * 1000;
                        log.info("Retrying job after {}ms - attempts: {}", backoffMs, attempts);
                        Thread.sleep(backoffMs);
                        
                        // Re-queue to main queue
                        requeueToMain(job);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (org.springframework.dao.QueryTimeoutException e) {
                // Redis timeout - this is expected when no jobs are available
                log.debug("Redis timeout (no retry jobs available), continuing...");
            } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
                log.error("Redis connection failure, waiting before retry", e);
                try {
                    Thread.sleep(5000); // Wait 5 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                log.error("Error processing retry queue", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // Processes a single moderation job: calls ML API, applies rules, saves results, sends WebSocket updates
    private void processJob(Map<String, Object> job) {
        UUID jobId = UUID.fromString((String) job.get("job_id"));
        UUID contentId = UUID.fromString((String) job.get("content_id"));
        String contentType = (String) job.get("content_type");
        String text = (String) job.get("text");
        String imageUrl = (String) job.get("image_url");
        int attempts = getIntValue(job, "attempts", 0);

        log.info("Processing job - job_id: {}, content_id: {}, type: {}, attempts: {}", 
                jobId, contentId, contentType, attempts);

        try {
            // Send QUEUED status (30%)
            webSocketService.sendProgressUpdate(contentId, "QUEUED", 30);

            // Update status to PROCESSING
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("Content not found: " + contentId));
            
            content.setStatus(Content.ContentStatus.PROCESSING);
            contentRepository.save(content);

            // Send PROCESSING status (60%)
            webSocketService.sendProgressUpdate(contentId, "PROCESSING", 60);

            // Track job
            trackJob(jobId, contentId, attempts, RedisKeys.MAIN_QUEUE, null);

            // Call ML API
            ModerationScores scores;
            if ("TEXT".equals(contentType)) {
                scores = mlModerationClient.callTextModeration(text);
            } else {
                scores = mlModerationClient.callImageModeration(imageUrl);
            }

            // Send AI_COMPLETED status (90%)
            webSocketService.sendProgressUpdate(contentId, "AI_COMPLETED", 90);

            // Save moderation result (this also updates content status)
            moderationService.saveModerationResult(contentId, scores);

            // Send DONE status (100%) with final label
            String finalLabel = content.getStatus() == Content.ContentStatus.SAFE ? "SAFE" : "FLAGGED";
            webSocketService.sendStatusUpdate(contentId, "DONE", finalLabel);
            webSocketService.sendProgressUpdate(contentId, "DONE", 100);

            log.info("Job completed successfully - job_id: {}, content_id: {}", jobId, contentId);

        } catch (Exception e) {
            log.error("Job processing failed - job_id: {}, content_id: {}, error: {}", 
                    jobId, contentId, e.getMessage(), e);

            // Send error via WebSocket
            webSocketService.sendError(contentId, e.getMessage());

            attempts++;
            
            if (attempts >= maxRetries) {
                moveToDLQ(job, e.getMessage());
            } else {
                // Move to retry queue
                queueService.pushToRetryQueue(jobId, contentId, contentType, text, imageUrl, attempts);
                trackJob(jobId, contentId, attempts, RedisKeys.RETRY_QUEUE, e.getMessage());
            }
        }
    }

    // Requeues a job from retry queue back to main queue after backoff delay
    private void requeueToMain(Map<String, Object> job) {
        UUID jobId = UUID.fromString((String) job.get("job_id"));
        UUID contentId = UUID.fromString((String) job.get("content_id"));
        String contentType = (String) job.get("content_type");
        String text = (String) job.get("text");
        String imageUrl = (String) job.get("image_url");
        int attempts = getIntValue(job, "attempts", 0);

        queueService.pushToMainQueue(jobId, contentId, contentType, text, imageUrl, attempts);
        trackJob(jobId, contentId, attempts, RedisKeys.MAIN_QUEUE, null);
    }

    // Moves a failed job to dead-letter queue and updates content status to ERROR
    private void moveToDLQ(Map<String, Object> job, String error) {
        UUID jobId = UUID.fromString((String) job.get("job_id"));
        UUID contentId = UUID.fromString((String) job.get("content_id"));
        String contentType = (String) job.get("content_type");
        String text = (String) job.get("text");
        String imageUrl = (String) job.get("image_url");
        int attempts = getIntValue(job, "attempts", 0);

        queueService.pushToDLQ(jobId, contentId, contentType, text, imageUrl, attempts, error);
        trackJob(jobId, contentId, attempts, RedisKeys.DLQ, error);

        // Update content status to ERROR
        contentRepository.findById(contentId).ifPresent(content -> {
            content.setStatus(Content.ContentStatus.ERROR);
            contentRepository.save(content);
        });
    }

    // Updates or creates job tracking record in database
    private void trackJob(UUID jobId, UUID contentId, int attempts, String queueName, String error) {
        ModerationJob job = jobRepository.findByContentId(contentId)
                .orElse(ModerationJob.builder()
                        .id(jobId)
                        .contentId(contentId)
                        .build());

        job.setAttempts(attempts);
        job.setQueueName(queueName);
        job.setLastError(error);
        jobRepository.save(job);
    }

    // Extracts integer value from map or returns default if not found or invalid type
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}

