package com.cache.config;

import java.time.Duration;

/**
 * 熔断器配置
 */
public record CircuitBreakerConfig(
    boolean enabled,
    int failureThreshold,
    Duration resetTimeout
) {
    public static CircuitBreakerConfig defaults() {
        return new CircuitBreakerConfig(true, 5, Duration.ofSeconds(30));
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean enabled = true;
        private int failureThreshold = 5;
        private Duration resetTimeout = Duration.ofSeconds(30);
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder failureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
            return this;
        }
        
        public Builder resetTimeout(Duration resetTimeout) {
            this.resetTimeout = resetTimeout;
            return this;
        }
        
        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(enabled, failureThreshold, resetTimeout);
        }
    }
}
