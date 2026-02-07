package com.cache.core;

import java.time.Instant;

/**
 * 缓存条目
 */
public record CacheEntry<V>(
    V value,
    Instant createdAt,
    Instant expiresAt,
    long version,
    boolean isNullValue,
    boolean isHotKey
) {
    public static <V> CacheEntry<V> of(V value, Instant expiresAt) {
        return new CacheEntry<>(value, Instant.now(), expiresAt, 1L, false, false);
    }
    
    public static <V> CacheEntry<V> ofNull(Instant expiresAt) {
        return new CacheEntry<>(null, Instant.now(), expiresAt, 1L, true, false);
    }
    
    public static <V> CacheEntry<V> ofHotKey(V value, Instant expiresAt) {
        return new CacheEntry<>(value, Instant.now(), expiresAt, 1L, false, true);
    }
    
    public CacheEntry<V> withVersion(long newVersion) {
        return new CacheEntry<>(value, createdAt, expiresAt, newVersion, isNullValue, isHotKey);
    }
    
    public CacheEntry<V> incrementVersion() {
        return withVersion(version + 1);
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
