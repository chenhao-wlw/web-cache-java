package com.cache.error;

import com.cache.core.CacheErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * 默认错误处理器实现
 */
public class DefaultErrorHandler implements ErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultErrorHandler.class);
    
    private final RetryConfig retryConfig;
    
    public DefaultErrorHandler(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }
    
    public DefaultErrorHandler() {
        this(RetryConfig.defaults());
    }
    
    @Override
    public CacheErrorResponse handle(CacheError error) {
        logger.warn("Handling cache error: type={}, message={}, key={}", 
                   error.type(), error.message(), error.key());
        
        // 检查是否应该熔断
        if (shouldCircuitBreak(error.type())) {
            logger.warn("Circuit break triggered for error type: {}", error.type());
            return CacheErrorResponse.circuitBreak();
        }
        
        // 检查是否可重试
        if (isRetryable(error.type())) {
            Duration delay = calculateRetryDelay(1);
            logger.debug("Error is retryable, delay: {}", delay);
            return CacheErrorResponse.retry(delay);
        }
        
        // 不可重试的错误
        return CacheErrorResponse.noRetry();
    }
    
    /**
     * 检查错误类型是否可重试
     */
    public boolean isRetryable(CacheErrorType errorType) {
        return retryConfig.retryableErrors().contains(errorType);
    }
    
    /**
     * 计算重试延迟（指数退避）
     */
    public Duration calculateRetryDelay(int attempt) {
        if (attempt <= 0) {
            return retryConfig.initialDelay();
        }
        
        double delayMs = retryConfig.initialDelay().toMillis() * 
                         Math.pow(retryConfig.backoffMultiplier(), attempt - 1);
        
        long cappedDelayMs = Math.min((long) delayMs, retryConfig.maxDelay().toMillis());
        
        return Duration.ofMillis(cappedDelayMs);
    }
    
    /**
     * 检查是否应该触发熔断
     */
    private boolean shouldCircuitBreak(CacheErrorType errorType) {
        return errorType == CacheErrorType.DATABASE_ERROR;
    }
}
