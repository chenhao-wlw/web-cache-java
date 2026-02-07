package com.cache.local;

import com.cache.config.LocalCacheConfig;
import com.cache.core.CacheStats;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 基于Caffeine的本地缓存实现
 */
public class CaffeineLocalCache<K, V> implements LocalCache<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(CaffeineLocalCache.class);
    
    private final Cache<K, V> cache;
    private final LocalCacheConfig config;
    
    public CaffeineLocalCache(LocalCacheConfig config) {
        this.config = config;
        
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
            .maximumSize(config.maxSize())
            .expireAfterWrite(config.defaultTtl().toMillis(), TimeUnit.MILLISECONDS)
            .removalListener((key, value, cause) -> {
                logger.debug("Cache entry removed: key={}, cause={}", key, cause);
            });
        
        if (config.recordStats()) {
            builder.recordStats();
        }
        
        this.cache = builder.build();
        logger.info("CaffeineLocalCache initialized with maxSize={}, defaultTtl={}", 
                    config.maxSize(), config.defaultTtl());
    }
    
    @Override
    public Optional<V> get(K key) {
        V value = cache.getIfPresent(key);
        if (value != null) {
            logger.debug("Cache hit: key={}", key);
            return Optional.of(value);
        }
        logger.debug("Cache miss: key={}", key);
        return Optional.empty();
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        cache.put(key, value);
        logger.debug("Cache put: key={}, ttl={}", key, ttl != null ? ttl : config.defaultTtl());
    }
    
    @Override
    public void delete(K key) {
        cache.invalidate(key);
        logger.debug("Cache delete: key={}", key);
    }
    
    @Override
    public void clear() {
        cache.invalidateAll();
        logger.info("Cache cleared");
    }
    
    @Override
    public long size() {
        return cache.estimatedSize();
    }
    
    @Override
    public CacheStats stats() {
        if (!config.recordStats()) {
            return CacheStats.empty();
        }
        
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = cache.stats();
        long totalRequests = caffeineStats.hitCount() + caffeineStats.missCount();
        double hitRate = totalRequests > 0 ? (double) caffeineStats.hitCount() / totalRequests : 0.0;
        
        return new CacheStats(
            caffeineStats.hitCount(),
            caffeineStats.missCount(),
            hitRate,
            caffeineStats.evictionCount(),
            caffeineStats.loadSuccessCount(),
            caffeineStats.loadFailureCount(),
            caffeineStats.averageLoadPenalty()
        );
    }
}
