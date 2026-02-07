package com.cache.error;

import java.time.Duration;

/**
 * 缓存错误响应
 */
public record CacheErrorResponse(
    boolean shouldRetry,
    Duration retryDelay,
    Object fallbackValue,
    boolean shouldCircuitBreak
) {
    public static CacheErrorResponse retry(Duration delay) {
        return new CacheErrorResponse(true, delay, null, false);
    }
    
    public static CacheErrorResponse fallback(Object value) {
        return new CacheErrorResponse(false, null, value, false);
    }
    
    public static CacheErrorResponse circuitBreak() {
        return new CacheErrorResponse(false, null, null, true);
    }
    
    public static CacheErrorResponse noRetry() {
        return new CacheErrorResponse(false, null, null, false);
    }
}
