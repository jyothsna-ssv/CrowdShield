package com.crowdshield.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimiter {
    
    @Value("${rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;
    
    @Value("${rate-limit.enabled:true}")
    private boolean enabled;
    
    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();
    
    // Checks if request is allowed based on rate limit configuration for the user
    public boolean allowRequest(String userId) {
        if (!enabled) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        RequestCounter counter = counters.computeIfAbsent(userId, k -> new RequestCounter());
        
        synchronized (counter) {
            if (currentTime - counter.windowStart > 60000) {
                counter.count.set(0);
                counter.windowStart = currentTime;
            }
            
            if (counter.count.get() >= requestsPerMinute) {
                log.warn("Rate limit exceeded for user: {}", userId);
                return false;
            }
            
            counter.count.incrementAndGet();
            return true;
        }
    }
    
    private static class RequestCounter {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();
    }
}

