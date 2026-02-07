package com.cache.facade;

import com.cache.bloom.BloomFilterService;
import com.cache.circuitbreaker.CircuitBreaker;
import com.cache.config.CacheConfig;
import com.cache.core.*;
import com.cache.distributed.DistributedCache;
import com.cache.hotkey.HotKeyDetector;
import com.cache.local.LocalCache;
import com.cache.metrics.CacheMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 多级缓存门面实现
 */
public class MultiLevelCacheFacade<K, V> implements CacheFacade<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheFacade.class);
    
    private final LocalCache<K, CacheEntry<V>> localCache;
    private final DistributedCache<K, CacheEntry<V>> distributedCache;
    private final BloomFilterService bloomFilter;
    private final HotKeyDetector hotKeyDetector;
    private final CircuitBreaker circuitBreaker;
    private final CacheMetrics metrics;
    private final CacheConfig config;
    
    private final Function<K, V> dataLoader;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong versionCounter;
    
    public MultiLevelCacheFacade(
            LocalCache<K, CacheEntry<V>> localCache,
            DistributedCache<K, CacheEntry<V>> distributedCache,
            BloomFilterService bloomFilter,
            HotKeyDetector hotKeyDetector,
            CircuitBreaker circuitBreaker,
            CacheMetrics metrics,
            CacheConfig config,
            Function<K, V> dataLoader) {
        
        this.localCache = localCache;
        this.distributedCache = distributedCache;
        this.bloomFilter = bloomFilter;
        this.hotKeyDetector = hotKeyDetector;
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;
        this.config = config;
        this.dataLoader = dataLoader;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.versionCounter = new AtomicLong(0);
        
        logger.info("MultiLevelCacheFacade initialized");
    }
    
    @Override
    public Optional<V> get(K key) {
        long startTime = System.nanoTime();
        String keyStr = key.toString();
        
        try {
            // 记录热点访问
            hotKeyDetector.recordAccess(keyStr);
            
            // 布隆过滤器检查（穿透防护）
            if (!bloomFilter.mightContain(keyStr)) {
                logger.debug("Key not in bloom filter: {}", key);
                metrics.recordEvent(CacheEventType.PENETRATION);
                return Optional.empty();
            }
            
            // L1缓存查询
            Optional<CacheEntry<V>> l1Result = localCache.get(key);
            if (l1Result.isPresent()) {
                CacheEntry<V> entry = l1Result.get();
                if (!entry.isExpired()) {
                    metrics.recordHit(CacheLevel.L1_LOCAL);
                    return entry.isNullValue() ? Optional.empty() : Optional.ofNullable(entry.value());
                }
            }
            metrics.recordMiss(CacheLevel.L1_LOCAL);
            
            // L2缓存查询
            Optional<CacheEntry<V>> l2Result = distributedCache.get(key);
            if (l2Result.isPresent()) {
                CacheEntry<V> entry = l2Result.get();
                if (!entry.isExpired()) {
                    // 回填L1
                    localCache.put(key, entry, config.localCache().defaultTtl());
                    metrics.recordHit(CacheLevel.L2_DISTRIBUTED);
                    return entry.isNullValue() ? Optional.empty() : Optional.ofNullable(entry.value());
                }
            }
            metrics.recordMiss(CacheLevel.L2_DISTRIBUTED);
            
            // 热点Key击穿防护
            if (hotKeyDetector.isHotKey(keyStr)) {
                return loadWithLock(key);
            }
            
            // 从数据源加载
            return loadFromDataSource(key);
            
        } finally {
            metrics.recordLatency(CacheOperation.GET, Duration.ofNanos(System.nanoTime() - startTime));
        }
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        long startTime = System.nanoTime();
        
        try {
            Duration effectiveTtl = ttl != null ? ttl : config.distributedCache().defaultTtl();
            Instant expiresAt = Instant.now().plus(effectiveTtl);
            
            CacheEntry<V> entry = new CacheEntry<>(
                value, Instant.now(), expiresAt, 
                versionCounter.incrementAndGet(), false, 
                hotKeyDetector.isHotKey(key.toString())
            );
            
            // 写入L1
            localCache.put(key, entry, config.localCache().defaultTtl());
            
            // 写入L2（带TTL随机化）
            distributedCache.putWithRandomTtl(key, entry, effectiveTtl, 
                config.distributedCache().ttlJitterPercent());
            
            // 更新布隆过滤器
            bloomFilter.add(key.toString());
            
            logger.debug("Cache put: key={}, ttl={}", key, effectiveTtl);
            
        } finally {
            metrics.recordLatency(CacheOperation.PUT, Duration.ofNanos(System.nanoTime() - startTime));
        }
    }
    
    @Override
    public void delete(K key) {
        long startTime = System.nanoTime();
        
        try {
            localCache.delete(key);
            distributedCache.delete(key);
            logger.debug("Cache delete: key={}", key);
            
        } finally {
            metrics.recordLatency(CacheOperation.DELETE, Duration.ofNanos(System.nanoTime() - startTime));
        }
    }
    
    @Override
    public Map<K, V> multiGet(Collection<K> keys) {
        Map<K, V> results = new HashMap<>();
        for (K key : keys) {
            get(key).ifPresent(v -> results.put(key, v));
        }
        return results;
    }
    
    @Override
    public void multiPut(Map<K, V> entries, Duration ttl) {
        for (Map.Entry<K, V> entry : entries.entrySet()) {
            put(entry.getKey(), entry.getValue(), ttl);
        }
    }
    
    @Override
    public void invalidate(K key) {
        // 延迟双删策略
        // 第一次删除
        delete(key);
        
        // 延迟后第二次删除
        scheduler.schedule(() -> {
            delete(key);
            logger.debug("Delayed delete executed: key={}", key);
        }, 500, TimeUnit.MILLISECONDS);
        
        logger.debug("Cache invalidate with double delete: key={}", key);
    }
    
    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        distributedCache.close();
        logger.info("MultiLevelCacheFacade closed");
    }
    
    /**
     * 带锁加载（击穿防护）
     */
    private Optional<V> loadWithLock(K key) {
        LockResult<CacheEntry<V>> lockResult = distributedCache.getWithLock(
            key, config.distributedCache().lockTimeout());
        
        try {
            if (lockResult.lockAcquired()) {
                // 获取锁成功，从数据源加载
                metrics.recordEvent(CacheEventType.BREAKDOWN);
                return loadFromDataSource(key);
            } else {
                // 获取锁失败，返回旧数据或等待
                if (lockResult.value() != null && !lockResult.value().isExpired()) {
                    return lockResult.value().isNullValue() ? 
                        Optional.empty() : Optional.ofNullable(lockResult.value().value());
                }
                
                // 等待一小段时间后重试
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 重试从缓存获取
                return distributedCache.get(key)
                    .filter(entry -> !entry.isExpired())
                    .map(entry -> entry.isNullValue() ? null : entry.value());
            }
        } finally {
            if (lockResult.lockAcquired() && lockResult.lockToken() != null) {
                distributedCache.releaseLock(key, lockResult.lockToken());
            }
        }
    }
    
    /**
     * 从数据源加载
     */
    private Optional<V> loadFromDataSource(K key) {
        return circuitBreaker.execute(
            () -> {
                V value = dataLoader.apply(key);
                
                if (value != null) {
                    put(key, value, null);
                    return Optional.of(value);
                } else {
                    // 缓存空值（穿透防护）
                    if (config.nullCache().enabled()) {
                        cacheNullValue(key);
                    }
                    return Optional.empty();
                }
            },
            () -> {
                logger.warn("Circuit breaker fallback for key: {}", key);
                return Optional.empty();
            }
        );
    }
    
    /**
     * 缓存空值
     */
    private void cacheNullValue(K key) {
        Duration nullTtl = config.nullCache().ttl();
        Instant expiresAt = Instant.now().plus(nullTtl);
        
        CacheEntry<V> nullEntry = CacheEntry.ofNull(expiresAt);
        
        localCache.put(key, nullEntry, nullTtl);
        distributedCache.put(key, nullEntry, nullTtl);
        
        logger.debug("Null value cached: key={}, ttl={}", key, nullTtl);
    }
}
