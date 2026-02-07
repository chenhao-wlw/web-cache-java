package com.cache.alert;

/**
 * 告警类型
 */
public enum AlertType {
    LOW_HIT_RATE,
    HIGH_LATENCY,
    CIRCUIT_BREAKER_OPEN,
    BLOOM_FILTER_REBUILD,
    CACHE_PENETRATION,
    CACHE_AVALANCHE_RISK
}
