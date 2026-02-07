package com.cache.alert;

import com.cache.core.CacheLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 默认告警服务实现
 */
public class DefaultAlertService implements AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultAlertService.class);
    
    private final List<Consumer<Alert>> alertHandlers;
    private final double defaultHitRateThreshold;
    
    public DefaultAlertService() {
        this(0.5); // 默认命中率阈值50%
    }
    
    public DefaultAlertService(double defaultHitRateThreshold) {
        this.alertHandlers = new CopyOnWriteArrayList<>();
        this.defaultHitRateThreshold = defaultHitRateThreshold;
        
        // 添加默认的日志处理器
        addAlertHandler(this::logAlert);
    }
    
    @Override
    public void sendAlert(Alert alert) {
        logger.info("Alert triggered: level={}, type={}, message={}", 
                   alert.level(), alert.type(), alert.message());
        
        for (Consumer<Alert> handler : alertHandlers) {
            try {
                handler.accept(alert);
            } catch (Exception e) {
                logger.error("Alert handler failed", e);
            }
        }
    }
    
    @Override
    public void checkHitRateAndAlert(CacheLevel level, double hitRate, double threshold) {
        double effectiveThreshold = threshold > 0 ? threshold : defaultHitRateThreshold;
        
        if (hitRate < effectiveThreshold) {
            String message = String.format(
                "Cache hit rate for %s is below threshold: %.2f%% < %.2f%%",
                level.name(), hitRate * 100, effectiveThreshold * 100
            );
            
            String details = String.format(
                "Current hit rate: %.4f, Threshold: %.4f, Level: %s",
                hitRate, effectiveThreshold, level.name()
            );
            
            AlertLevel alertLevel = hitRate < effectiveThreshold * 0.5 ? 
                AlertLevel.CRITICAL : AlertLevel.WARNING;
            
            Alert alert = new Alert(alertLevel, AlertType.LOW_HIT_RATE, message, details, 
                                   java.time.Instant.now());
            sendAlert(alert);
        }
    }
    
    /**
     * 添加告警处理器
     */
    public void addAlertHandler(Consumer<Alert> handler) {
        alertHandlers.add(handler);
    }
    
    /**
     * 移除告警处理器
     */
    public void removeAlertHandler(Consumer<Alert> handler) {
        alertHandlers.remove(handler);
    }
    
    /**
     * 默认日志处理器
     */
    private void logAlert(Alert alert) {
        switch (alert.level()) {
            case CRITICAL:
                logger.error("[ALERT-CRITICAL] {} - {}", alert.type(), alert.message());
                break;
            case WARNING:
                logger.warn("[ALERT-WARNING] {} - {}", alert.type(), alert.message());
                break;
            case INFO:
            default:
                logger.info("[ALERT-INFO] {} - {}", alert.type(), alert.message());
                break;
        }
    }
}
