package com.cache.alert;

import com.cache.core.CacheLevel;

/**
 * 告警服务接口
 */
public interface AlertService {
    
    /**
     * 发送告警
     */
    void sendAlert(Alert alert);
    
    /**
     * 检查命中率并触发告警
     */
    void checkHitRateAndAlert(CacheLevel level, double hitRate, double threshold);
}
