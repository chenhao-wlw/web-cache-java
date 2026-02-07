package com.cache.circuitbreaker;

import com.cache.core.CircuitBreakerStats;
import com.cache.core.CircuitState;

import java.util.function.Supplier;

/**
 * 熔断器接口
 */
public interface CircuitBreaker {
    
    /**
     * 执行受保护的操作
     */
    <T> T execute(Supplier<T> operation, Supplier<T> fallback);
    
    /**
     * 获取当前状态
     */
    CircuitState getState();
    
    /**
     * 手动重置
     */
    void reset();
    
    /**
     * 获取统计信息
     */
    CircuitBreakerStats stats();
}
