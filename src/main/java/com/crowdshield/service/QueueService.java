package com.crowdshield.service;

import com.crowdshield.queue.RedisKeys;
import com.crowdshield.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class QueueService {

    private final RedisTemplate<String, String> redisTemplate;

    public QueueService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Adds a new moderation job to the main processing queue in Redis
    public void pushToMainQueue(UUID jobId, UUID contentId, String contentType, String text, String imageUrl, int attempts) {
        Map<String, Object> job = Map.of(
                "job_id", jobId.toString(),
                "content_id", contentId.toString(),
                "content_type", contentType,
                "text", text != null ? text : "",
                "image_url", imageUrl != null ? imageUrl : "",
                "attempts", attempts
        );

        String jobJson = JsonUtils.toJson(job);
        redisTemplate.opsForList().leftPush(RedisKeys.MAIN_QUEUE, jobJson);
        
        log.info("Pushed job to main queue - job_id: {}, content_id: {}, attempts: {}", jobId, contentId, attempts);
    }

    // Moves a failed job to the retry queue for processing with exponential backoff
    public void pushToRetryQueue(UUID jobId, UUID contentId, String contentType, String text, String imageUrl, int attempts) {
        Map<String, Object> job = Map.of(
                "job_id", jobId.toString(),
                "content_id", contentId.toString(),
                "content_type", contentType,
                "text", text != null ? text : "",
                "image_url", imageUrl != null ? imageUrl : "",
                "attempts", attempts
        );

        String jobJson = JsonUtils.toJson(job);
        redisTemplate.opsForList().leftPush(RedisKeys.RETRY_QUEUE, jobJson);
        
        log.info("Pushed job to retry queue - job_id: {}, content_id: {}, attempts: {}", jobId, contentId, attempts);
    }

    // Moves a permanently failed job to the dead-letter queue for manual review
    public void pushToDLQ(UUID jobId, UUID contentId, String contentType, String text, String imageUrl, int attempts, String error) {
        Map<String, Object> job = Map.of(
                "job_id", jobId.toString(),
                "content_id", contentId.toString(),
                "content_type", contentType,
                "text", text != null ? text : "",
                "image_url", imageUrl != null ? imageUrl : "",
                "attempts", attempts,
                "error", error != null ? error : ""
        );

        String jobJson = JsonUtils.toJson(job);
        redisTemplate.opsForList().leftPush(RedisKeys.DLQ, jobJson);
        
        log.error("Pushed job to DLQ - job_id: {}, content_id: {}, attempts: {}, error: {}", jobId, contentId, attempts, error);
    }

    // Removes and returns a job from the specified queue, blocking for the timeout duration if queue is empty
    @SuppressWarnings("unchecked")
    public Map<String, Object> popFromQueue(String queueName, long timeoutSeconds) {
        String result = redisTemplate.opsForList().rightPop(queueName, timeoutSeconds, TimeUnit.SECONDS);
        
        if (result == null) {
            return null;
        }

        Map<String, Object> job = JsonUtils.fromJson(result, Map.class);
        log.info("Popped job from queue: {} - job_id: {}", queueName, job != null ? job.get("job_id") : "null");
        
        return job;
    }

    // Returns the current number of jobs waiting in the specified queue
    public long getQueueSize(String queueName) {
        Long size = redisTemplate.opsForList().size(queueName);
        return size != null ? size : 0;
    }
}

