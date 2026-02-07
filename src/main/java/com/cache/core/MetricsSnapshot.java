package com.cache.core;

import java.time.Instant;
import java.util.Map;

/**
 * 监控指标快照
 */
public record MetricsSnapshot(
    Instant timestamp,
    Map<CacheLevel, Long> hitCounts,
    Map<CacheLevel, Long> missCounts,
    Map<CacheLevel, Double> hitRates,
    Map<CacheOperation, Double> averageLatencies,
    Map<CacheEventType, Long> eventCounts
) {
}
