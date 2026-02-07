package com.cache.error;

import com.cache.core.CacheErrorType;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

/**
 * 重试配置
 */
public record RetryConfig(
    int maxAttempts,
    Duration initialDelay,
    Duration maxDelay,
    double backoffMultiplier,
    Set<CacheErrorType> retryableErrors
) {
    public static RetryConfig defaults() {
        return new RetryConfig(
            3,
            Duration.ofMillis(100),
            Duration.ofSeconds(5),
            2.0,
            EnumSet.of(
                CacheErrorType.L2_CONNECTION_ERROR,
                CacheErrorType.L2_TIMEOUT,
                CacheErrorType.DATABASE_ERROR
            )
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(5);
        private double backoffMultiplier = 2.0;
        private Set<CacheErrorType> retryableErrors = EnumSet.of(
            CacheErrorType.L2_CONNECTION_ERROR,
            CacheErrorType.L2_TIMEOUT,
            CacheErrorType.DATABASE_ERROR
        );
        
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }
        
        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }
        
        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }
        
        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }
        
        public Builder retryableErrors(Set<CacheErrorType> retryableErrors) {
            this.retryableErrors = retryableErrors;
            return this;
        }
        
        public RetryConfig build() {
            return new RetryConfig(maxAttempts, initialDelay, maxDelay, 
                                   backoffMultiplier, retryableErrors);
        }
    }
}
