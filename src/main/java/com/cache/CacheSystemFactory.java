package com.cache;

import com.cache.bloom.BloomFilterService;
import com.cache.bloom.GuavaBloomFilterService;
import com.cache.circuitbreaker.CircuitBreaker;
import com.cache.circuitbreaker.DefaultCircuitBreaker;
import com.cache.config.CacheConfig;
import com.cache.core.CacheEntry;
import com.cache.distributed.DistributedCache;
import com.cache.distributed.RedisDistributedCache;
import com.cache.facade.CacheFacade;
import com.cache.facade.MultiLevelCacheFacade;
import com.cache.hotkey.HotKeyDetector;
import com.cache.hotkey.SlidingWindowHotKeyDetector;
import com.cache.local.CaffeineLocalCache;
import com.cache.local.LocalCache;
import com.cache.metrics.CacheMetrics;
import com.cache.metrics.DefaultCacheMetrics;

import java.util.function.Function;

/**
 * 缓存系统工厂
 */
public class CacheSystemFactory {
    
    /**
     * 创建完整的多级缓存系统
     */
    public static <K, V> CacheFacade<K, V> create(
            CacheConfig config,
            Class<CacheEntry<V>> entryType,
            Function<K, V> dataLoader) {
        
        // 创建本地缓存
        LocalCache<K, CacheEntry<V>> localCache = new CaffeineLocalCache<>(config.localCache());
        
        // 创建分布式缓存
        @SuppressWarnings("unchecked")
        DistributedCache<K, CacheEntry<V>> distributedCache = 
            (DistributedCache<K, CacheEntry<V>>) new RedisDistributedCache<>(
                config.distributedCache(), entryType);
        
        // 创建布隆过滤器
        BloomFilterService bloomFilter = new GuavaBloomFilterService(config.bloomFilter());
        
        // 创建热点Key检测器
        HotKeyDetector hotKeyDetector = new SlidingWindowHotKeyDetector(config.hotKeyDetection());
        
        // 创建熔断器
        CircuitBreaker circuitBreaker = new DefaultCircuitBreaker(config.circuitBreaker());
        
        // 创建监控
        CacheMetrics metrics = new DefaultCacheMetrics();
        
        // 创建缓存门面
        return new MultiLevelCacheFacade<>(
            localCache,
            distributedCache,
            bloomFilter,
            hotKeyDetector,
            circuitBreaker,
            metrics,
            config,
            dataLoader
        );
    }
    
    /**
     * 使用默认配置创建缓存系统
     */
    public static <K, V> CacheFacade<K, V> createWithDefaults(
            Class<CacheEntry<V>> entryType,
            Function<K, V> dataLoader) {
        return create(CacheConfig.defaults(), entryType, dataLoader);
    }
}
