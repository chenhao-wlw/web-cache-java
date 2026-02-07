package com.cache.error;

import com.cache.core.CacheErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 重试执行器
 */
public class RetryExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryExecutor.class);
    
    private final RetryConfig config;
    private final DefaultErrorHandler errorHandler;
    
    public RetryExecutor(RetryConfig config) {
        this.config = config;
        this.errorHandler = new DefaultErrorHandler(config);
    }
    
    public RetryExecutor() {
        this(RetryConfig.defaults());
    }
    
    /**
     * 执行带重试的操作
     */
    public <T> T executeWithRetry(Supplier<T> operation, CacheErrorType errorType) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < config.maxAttempts()) {
            attempt++;
            
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                logger.warn("Operation failed, attempt {}/{}: {}", 
                           attempt, config.maxAttempts(), e.getMessage());
                
                if (!errorHandler.isRetryable(errorType) || attempt >= config.maxAttempts()) {
                    break;
                }
                
                Duration delay = errorHandler.calculateRetryDelay(attempt);
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        throw new RuntimeException("Operation failed after " + attempt + " attempts", lastException);
    }
    
    /**
     * 执行带重试和降级的操作
     */
    public <T> T executeWithRetryAndFallback(
            Supplier<T> operation, 
            Supplier<T> fallback, 
            CacheErrorType errorType) {
        try {
            return executeWithRetry(operation, errorType);
        } catch (Exception e) {
            logger.warn("All retries failed, executing fallback: {}", e.getMessage());
            return fallback.get();
        }
    }
}
