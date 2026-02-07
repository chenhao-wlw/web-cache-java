package com.cache.core;

/**
 * 布隆过滤器统计信息
 */
public record BloomFilterStats(
    long expectedInsertions,
    long actualInsertions,
    double falsePositiveRate,
    long bitSize,
    int hashFunctions
) {
    public static BloomFilterStats empty() {
        return new BloomFilterStats(0, 0, 0.0, 0, 0);
    }
}
