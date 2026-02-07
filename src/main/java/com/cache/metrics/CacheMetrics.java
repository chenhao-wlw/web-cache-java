package com.cache.metrics;

import com.cache.core.CacheEventType;
import com.cache.core.CacheLevel;
import com.cache.core.CacheOperation;
import com.cache.core.MetricsSnapshot;

import java.time.Duration;

/**
 * 缓存监控接口
 */
public interface CacheMetrics {
    
    /**
     * 记录缓存命中
     */
    void recordHit(CacheLevel cacheLevel);
    
    /**
     * 记录缓存未命中
     */
    void recordMiss(CacheLevel cacheLevel);
    
    /**
     * 记录操作延迟
     */
    void recordLatency(CacheOperation operation, Duration latency);
    
    /**
     * 记录异常事件
     */
    void recordEvent(CacheEventType eventType);
    
    /**
     * 获取指标快照
     */
    MetricsSnapshot snapshot();
    
    /**
     * 导出Prometheus格式
     */
    String exportPrometheus();
}
