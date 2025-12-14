package com.crowdshield.queue;

public class RedisKeys {
    
    public static final String MAIN_QUEUE = "moderation:jobs";
    public static final String RETRY_QUEUE = "moderation:retry";
    public static final String DLQ = "moderation:dlq";
    
    private RedisKeys() {
        // Utility class
    }
}

