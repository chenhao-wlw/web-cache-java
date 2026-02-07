package com.cache.hotkey;

import java.time.Duration;
import java.util.Set;

/**
 * 热点Key检测器接口
 */
public interface HotKeyDetector {
    
    /**
     * 记录key访问
     */
    void recordAccess(String key);
    
    /**
     * 检查是否为热点key
     */
    boolean isHotKey(String key);
    
    /**
     * 获取当前热点key列表
     */
    Set<String> getHotKeys();
    
    /**
     * 配置热点阈值
     */
    void setThreshold(int accessCount, Duration timeWindow);
}
