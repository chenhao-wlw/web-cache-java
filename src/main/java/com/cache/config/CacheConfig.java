package com.cache.config;

import java.time.Duration;

/**
 * 缓存系统配置
 */
public record CacheConfig(
    LocalCacheConfig localCache,
    DistributedCacheConfig distributedCache,
    BloomFilterConfig bloomFilter,
    NullCacheConfig nullCache,
    HotKeyDetectionConfig hotKeyDetection,
    CircuitBreakerConfig circuitBreaker
) {
    public static CacheConfig defaults() {
        return new CacheConfig(
            LocalCacheConfig.defaults(),
            DistributedCacheConfig.defaults(),
            BloomFilterConfig.defaults(),
            NullCacheConfig.defaults(),
            HotKeyDetectionConfig.defaults(),
            CircuitBreakerConfig.defaults()
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private LocalCacheConfig localCache = LocalCacheConfig.defaults();
        private DistributedCacheConfig distributedCache = DistributedCacheConfig.defaults();
        private BloomFilterConfig bloomFilter = BloomFilterConfig.defaults();
        private NullCacheConfig nullCache = NullCacheConfig.defaults();
        private HotKeyDetectionConfig hotKeyDetection = HotKeyDetectionConfig.defaults();
        private CircuitBreakerConfig circuitBreaker = CircuitBreakerConfig.defaults();
        
        public Builder localCache(LocalCacheConfig config) {
            this.localCache = config;
            return this;
        }
        
        public Builder distributedCache(DistributedCacheConfig config) {
            this.distributedCache = config;
            return this;
        }
        
        public Builder bloomFilter(BloomFilterConfig config) {
            this.bloomFilter = config;
            return this;
        }
        
        public Builder nullCache(NullCacheConfig config) {
            this.nullCache = config;
            return this;
        }
        
        public Builder hotKeyDetection(HotKeyDetectionConfig config) {
            this.hotKeyDetection = config;
            return this;
        }
        
        public Builder circuitBreaker(CircuitBreakerConfig config) {
            this.circuitBreaker = config;
            return this;
        }
        
        public CacheConfig build() {
            return new CacheConfig(localCache, distributedCache, bloomFilter, 
                                   nullCache, hotKeyDetection, circuitBreaker);
        }
    }
}
