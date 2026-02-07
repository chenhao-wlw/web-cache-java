package com.cache.circuitbreaker;

import com.cache.config.CircuitBreakerConfig;
import com.cache.core.CircuitBreakerStats;
import com.cache.core.CircuitState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 默认熔断器实现
 */
public class DefaultCircuitBreaker implements CircuitBreaker {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultCircuitBreaker.class);
    
    private final CircuitBreakerConfig config;
    private final AtomicReference<CircuitState> state;
    private final AtomicInteger failureCount;
    private final AtomicInteger successCount;
    private final AtomicReference<Instant> lastFailureTime;
    private final AtomicReference<Instant> lastSuccessTime;
    private final AtomicReference<Instant> openTime;
    
    public DefaultCircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
        this.state = new AtomicReference<>(CircuitState.CLOSED);
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicReference<>(null);
        this.lastSuccessTime = new AtomicReference<>(null);
        this.openTime = new AtomicReference<>(null);
        
        logger.info("DefaultCircuitBreaker initialized with failureThreshold={}, resetTimeout={}", 
                    config.failureThreshold(), config.resetTimeout());
    }
    
    @Override
    public <T> T execute(Supplier<T> operation, Supplier<T> fallback) {
        if (!config.enabled()) {
            return operation.get();
        }
        
        CircuitState currentState = state.get();
        
        switch (currentState) {
            case OPEN:
                // 检查是否可以转换到半开状态
                if (shouldAttemptReset()) {
                    return attemptHalfOpen(operation, fallback);
                }
                logger.debug("Circuit is OPEN, returning fallback");
                return fallback.get();
                
            case HALF_OPEN:
                return attemptHalfOpen(operation, fallback);
                
            case CLOSED:
            default:
                return attemptClosed(operation, fallback);
        }
    }
    
    @Override
    public CircuitState getState() {
        return state.get();
    }
    
    @Override
    public void reset() {
        state.set(CircuitState.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        openTime.set(null);
        logger.info("Circuit breaker reset to CLOSED state");
    }
    
    @Override
    public CircuitBreakerStats stats() {
        return new CircuitBreakerStats(
            state.get(),
            failureCount.get(),
            successCount.get(),
            lastFailureTime.get(),
            lastSuccessTime.get()
        );
    }
    
    private <T> T attemptClosed(Supplier<T> operation, Supplier<T> fallback) {
        try {
            T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            logger.warn("Operation failed in CLOSED state: {}", e.getMessage());
            return fallback.get();
        }
    }
    
    private <T> T attemptHalfOpen(Supplier<T> operation, Supplier<T> fallback) {
        // 尝试转换到半开状态
        if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
            logger.info("Circuit transitioned to HALF_OPEN state");
        }
        
        try {
            T result = operation.get();
            onHalfOpenSuccess();
            return result;
        } catch (Exception e) {
            onHalfOpenFailure();
            logger.warn("Operation failed in HALF_OPEN state: {}", e.getMessage());
            return fallback.get();
        }
    }
    
    private void onSuccess() {
        successCount.incrementAndGet();
        lastSuccessTime.set(Instant.now());
        // 在CLOSED状态下成功，重置失败计数
        failureCount.set(0);
    }
    
    private void onFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(Instant.now());
        
        if (failures >= config.failureThreshold()) {
            if (state.compareAndSet(CircuitState.CLOSED, CircuitState.OPEN)) {
                openTime.set(Instant.now());
                logger.warn("Circuit transitioned to OPEN state after {} failures", failures);
            }
        }
    }
    
    private void onHalfOpenSuccess() {
        successCount.incrementAndGet();
        lastSuccessTime.set(Instant.now());
        
        // 半开状态下成功，转换回关闭状态
        if (state.compareAndSet(CircuitState.HALF_OPEN, CircuitState.CLOSED)) {
            failureCount.set(0);
            openTime.set(null);
            logger.info("Circuit transitioned to CLOSED state after successful probe");
        }
    }
    
    private void onHalfOpenFailure() {
        failureCount.incrementAndGet();
        lastFailureTime.set(Instant.now());
        
        // 半开状态下失败，转换回打开状态
        if (state.compareAndSet(CircuitState.HALF_OPEN, CircuitState.OPEN)) {
            openTime.set(Instant.now());
            logger.warn("Circuit transitioned back to OPEN state after failed probe");
        }
    }
    
    private boolean shouldAttemptReset() {
        Instant open = openTime.get();
        if (open == null) {
            return true;
        }
        
        Instant resetTime = open.plus(config.resetTimeout());
        return Instant.now().isAfter(resetTime);
    }
}
