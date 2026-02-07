package com.cache.metrics;

import com.cache.core.CacheEventType;
import com.cache.core.CacheLevel;
import com.cache.core.CacheOperation;
import com.cache.core.MetricsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * 默认缓存监控实现
 */
public class DefaultCacheMetrics implements CacheMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultCacheMetrics.class);
    
    private final Map<CacheLevel, LongAdder> hitCounts;
    private final Map<CacheLevel, LongAdder> missCounts;
    private final Map<CacheOperation, LatencyRecorder> latencyRecorders;
    private final Map<CacheEventType, LongAdder> eventCounts;
    
    public DefaultCacheMetrics() {
        this.hitCounts = new ConcurrentHashMap<>();
        this.missCounts = new ConcurrentHashMap<>();
        this.latencyRecorders = new ConcurrentHashMap<>();
        this.eventCounts = new ConcurrentHashMap<>();
        
        // 初始化所有枚举值的计数器
        for (CacheLevel level : CacheLevel.values()) {
            hitCounts.put(level, new LongAdder());
            missCounts.put(level, new LongAdder());
        }
        
        for (CacheOperation op : CacheOperation.values()) {
            latencyRecorders.put(op, new LatencyRecorder());
        }
        
        for (CacheEventType event : CacheEventType.values()) {
            eventCounts.put(event, new LongAdder());
        }
        
        logger.info("DefaultCacheMetrics initialized");
    }
    
    @Override
    public void recordHit(CacheLevel cacheLevel) {
        hitCounts.get(cacheLevel).increment();
        logger.debug("Cache hit recorded: level={}", cacheLevel);
    }
    
    @Override
    public void recordMiss(CacheLevel cacheLevel) {
        missCounts.get(cacheLevel).increment();
        logger.debug("Cache miss recorded: level={}", cacheLevel);
    }
    
    @Override
    public void recordLatency(CacheOperation operation, Duration latency) {
        latencyRecorders.get(operation).record(latency.toNanos());
        logger.debug("Latency recorded: operation={}, latency={}ns", operation, latency.toNanos());
    }
    
    @Override
    public void recordEvent(CacheEventType eventType) {
        eventCounts.get(eventType).increment();
        logger.debug("Event recorded: type={}", eventType);
    }
    
    @Override
    public MetricsSnapshot snapshot() {
        Instant now = Instant.now();
        
        Map<CacheLevel, Long> hits = hitCounts.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()));
        
        Map<CacheLevel, Long> misses = missCounts.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()));
        
        Map<CacheLevel, Double> hitRates = new EnumMap<>(CacheLevel.class);
        for (CacheLevel level : CacheLevel.values()) {
            long h = hits.getOrDefault(level, 0L);
            long m = misses.getOrDefault(level, 0L);
            long total = h + m;
            hitRates.put(level, total > 0 ? (double) h / total : 0.0);
        }
        
        Map<CacheOperation, Double> avgLatencies = latencyRecorders.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAverageNanos()));
        
        Map<CacheEventType, Long> events = eventCounts.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()));
        
        return new MetricsSnapshot(now, hits, misses, hitRates, avgLatencies, events);
    }
    
    @Override
    public String exportPrometheus() {
        StringBuilder sb = new StringBuilder();
        
        // 缓存命中计数
        sb.append("# HELP cache_hits_total Total number of cache hits\n");
        sb.append("# TYPE cache_hits_total counter\n");
        for (CacheLevel level : CacheLevel.values()) {
            sb.append(String.format("cache_hits_total{level=\"%s\"} %d\n", 
                level.name().toLowerCase(), hitCounts.get(level).sum()));
        }
        
        // 缓存未命中计数
        sb.append("# HELP cache_misses_total Total number of cache misses\n");
        sb.append("# TYPE cache_misses_total counter\n");
        for (CacheLevel level : CacheLevel.values()) {
            sb.append(String.format("cache_misses_total{level=\"%s\"} %d\n", 
                level.name().toLowerCase(), missCounts.get(level).sum()));
        }
        
        // 缓存命中率
        sb.append("# HELP cache_hit_rate Cache hit rate\n");
        sb.append("# TYPE cache_hit_rate gauge\n");
        for (CacheLevel level : CacheLevel.values()) {
            long hits = hitCounts.get(level).sum();
            long misses = missCounts.get(level).sum();
            long total = hits + misses;
            double rate = total > 0 ? (double) hits / total : 0.0;
            sb.append(String.format("cache_hit_rate{level=\"%s\"} %.4f\n", 
                level.name().toLowerCase(), rate));
        }
        
        // 操作延迟
        sb.append("# HELP cache_operation_latency_nanoseconds Average operation latency in nanoseconds\n");
        sb.append("# TYPE cache_operation_latency_nanoseconds gauge\n");
        for (CacheOperation op : CacheOperation.values()) {
            sb.append(String.format("cache_operation_latency_nanoseconds{operation=\"%s\"} %.2f\n", 
                op.name().toLowerCase(), latencyRecorders.get(op).getAverageNanos()));
        }
        
        // 事件计数
        sb.append("# HELP cache_events_total Total number of cache events\n");
        sb.append("# TYPE cache_events_total counter\n");
        for (CacheEventType event : CacheEventType.values()) {
            sb.append(String.format("cache_events_total{type=\"%s\"} %d\n", 
                event.name().toLowerCase(), eventCounts.get(event).sum()));
        }
        
        return sb.toString();
    }
    
    /**
     * 延迟记录器
     */
    private static class LatencyRecorder {
        private final LongAdder totalNanos = new LongAdder();
        private final LongAdder count = new LongAdder();
        
        public void record(long nanos) {
            totalNanos.add(nanos);
            count.increment();
        }
        
        public double getAverageNanos() {
            long c = count.sum();
            return c > 0 ? (double) totalNanos.sum() / c : 0.0;
        }
    }
}
