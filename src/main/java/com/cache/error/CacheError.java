package com.cache.error;

import com.cache.core.CacheErrorType;

import java.time.Instant;

/**
 * 缓存错误
 */
public record CacheError(
    CacheErrorType type,
    String message,
    String key,
    Throwable cause,
    Instant timestamp
) {
    public static CacheError of(CacheErrorType type, String message, String key, Throwable cause) {
        return new CacheError(type, message, key, cause, Instant.now());
    }
    
    public static CacheError of(CacheErrorType type, String message, String key) {
        return new CacheError(type, message, key, null, Instant.now());
    }
    
    public static CacheError of(CacheErrorType type, String message) {
        return new CacheError(type, message, null, null, Instant.now());
    }
}
