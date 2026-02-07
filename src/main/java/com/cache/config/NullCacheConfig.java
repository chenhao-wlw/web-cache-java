package com.cache.config;

import java.time.Duration;

/**
 * 空值缓存配置
 */
public record NullCacheConfig(
    boolean enabled,
    Duration ttl
) {
    public static NullCacheConfig defaults() {
        return new NullCacheConfig(true, Duration.ofMinutes(5));
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean enabled = true;
        private Duration ttl = Duration.ofMinutes(5);
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder ttl(Duration ttl) {
            if (ttl.toMinutes() > 5) {
                throw new IllegalArgumentException("Null cache TTL should not exceed 5 minutes");
            }
            this.ttl = ttl;
            return this;
        }
        
        public NullCacheConfig build() {
            return new NullCacheConfig(enabled, ttl);
        }
    }
}
