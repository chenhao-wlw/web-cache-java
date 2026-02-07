package com.cache.core;

/**
 * 缓存统计信息
 */
public record CacheStats(
    long hitCount,
    long missCount,
    double hitRate,
    long evictionCount,
    long loadSuccessCount,
    long loadFailureCount,
    double averageLoadPenaltyNanos
) {
    public static CacheStats empty() {
        return new CacheStats(0, 0, 0.0, 0, 0, 0, 0.0);
    }
}
