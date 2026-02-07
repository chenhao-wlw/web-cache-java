package com.cache.core;

/**
 * 缓存事件类型枚举
 */
public enum CacheEventType {
    /** 缓存穿透事件 */
    PENETRATION,
    /** 缓存雪崩风险事件 */
    AVALANCHE_RISK,
    /** 缓存击穿事件 */
    BREAKDOWN,
    /** 熔断器打开事件 */
    CIRCUIT_OPEN
}
