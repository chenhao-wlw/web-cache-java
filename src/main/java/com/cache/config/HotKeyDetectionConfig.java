package com.cache.config;

import java.time.Duration;

/**
 * 热点Key检测配置
 */
public record HotKeyDetectionConfig(
    boolean enabled,
    int threshold,
    Duration timeWindow
) {
    public static HotKeyDetectionConfig defaults() {
        return new HotKeyDetectionConfig(true, 100, Duration.ofSeconds(60));
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean enabled = true;
        private int threshold = 100;
        private Duration timeWindow = Duration.ofSeconds(60);
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder threshold(int threshold) {
            this.threshold = threshold;
            return this;
        }
        
        public Builder timeWindow(Duration timeWindow) {
            this.timeWindow = timeWindow;
            return this;
        }
        
        public HotKeyDetectionConfig build() {
            return new HotKeyDetectionConfig(enabled, threshold, timeWindow);
        }
    }
}
