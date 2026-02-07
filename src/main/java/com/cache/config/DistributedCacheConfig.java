package com.cache.config;

import java.time.Duration;

/**
 * 分布式缓存配置
 */
public record DistributedCacheConfig(
    String redisUri,
    Duration defaultTtl,
    int ttlJitterPercent,
    Duration lockTimeout
) {
    public static DistributedCacheConfig defaults() {
        return new DistributedCacheConfig(
            "redis://localhost:6379",
            Duration.ofMinutes(5),
            20,
            Duration.ofSeconds(3)
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String redisUri = "redis://localhost:6379";
        private Duration defaultTtl = Duration.ofMinutes(5);
        private int ttlJitterPercent = 20;
        private Duration lockTimeout = Duration.ofSeconds(3);
        
        public Builder redisUri(String redisUri) {
            this.redisUri = redisUri;
            return this;
        }
        
        public Builder defaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
            return this;
        }
        
        public Builder ttlJitterPercent(int ttlJitterPercent) {
            if (ttlJitterPercent < 10 || ttlJitterPercent > 30) {
                throw new IllegalArgumentException("TTL jitter percent must be between 10 and 30");
            }
            this.ttlJitterPercent = ttlJitterPercent;
            return this;
        }
        
        public Builder lockTimeout(Duration lockTimeout) {
            this.lockTimeout = lockTimeout;
            return this;
        }
        
        public DistributedCacheConfig build() {
            return new DistributedCacheConfig(redisUri, defaultTtl, ttlJitterPercent, lockTimeout);
        }
    }
}
