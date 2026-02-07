package com.cache.core;

import java.time.Instant;

/**
 * 熔断器统计信息
 */
public record CircuitBreakerStats(
    CircuitState state,
    int failureCount,
    int successCount,
    Instant lastFailureTime,
    Instant lastSuccessTime
) {
    public static CircuitBreakerStats initial() {
        return new CircuitBreakerStats(CircuitState.CLOSED, 0, 0, null, null);
    }
}
