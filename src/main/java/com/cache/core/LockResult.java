package com.cache.core;

/**
 * 分布式锁获取结果
 */
public record LockResult<V>(
    V value,
    boolean lockAcquired,
    String lockToken
) {
    public static <V> LockResult<V> acquired(V value, String token) {
        return new LockResult<>(value, true, token);
    }
    
    public static <V> LockResult<V> notAcquired(V value) {
        return new LockResult<>(value, false, null);
    }
    
    public static <V> LockResult<V> notAcquired() {
        return new LockResult<>(null, false, null);
    }
}
