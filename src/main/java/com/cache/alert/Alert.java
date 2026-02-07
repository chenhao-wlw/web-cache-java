package com.cache.alert;

import java.time.Instant;

/**
 * 告警信息
 */
public record Alert(
    AlertLevel level,
    AlertType type,
    String message,
    String details,
    Instant timestamp
) {
    public static Alert warning(AlertType type, String message, String details) {
        return new Alert(AlertLevel.WARNING, type, message, details, Instant.now());
    }
    
    public static Alert critical(AlertType type, String message, String details) {
        return new Alert(AlertLevel.CRITICAL, type, message, details, Instant.now());
    }
    
    public static Alert info(AlertType type, String message, String details) {
        return new Alert(AlertLevel.INFO, type, message, details, Instant.now());
    }
}
