package com.cache.core;

/**
 * 熔断器状态枚举
 */
public enum CircuitState {
    /** 关闭状态 - 正常工作 */
    CLOSED,
    /** 打开状态 - 熔断中 */
    OPEN,
    /** 半开状态 - 尝试恢复 */
    HALF_OPEN
}
