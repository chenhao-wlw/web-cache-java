package com.cache.config;

import java.time.Duration;

/**
 * 本地缓存配置
 */
public record LocalCacheConfig(
    int maxSize,
    Duration defaultTtl,
    boolean recordStats
) {
    public static LocalCacheConfig defaults() {
        return new LocalCacheConfig(10000, Duration.ofSeconds(60), true);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int maxSize = 10000;
        private Duration defaultTtl = Duration.ofSeconds(60);
        private boolean recordStats = true;
        
        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
        
        public Builder defaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
            return this;
        }
        
        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }
        
        public LocalCacheConfig build() {
            return new LocalCacheConfig(maxSize, defaultTtl, recordStats);
        }
    }
}
