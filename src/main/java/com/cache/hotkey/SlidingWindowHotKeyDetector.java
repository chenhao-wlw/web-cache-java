package com.cache.hotkey;

import com.cache.config.HotKeyDetectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 基于滑动窗口的热点Key检测器实现
 */
public class SlidingWindowHotKeyDetector implements HotKeyDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(SlidingWindowHotKeyDetector.class);
    
    private final ConcurrentHashMap<String, AccessRecord> accessRecords;
    private final Set<String> hotKeys;
    
    private volatile int threshold;
    private volatile long timeWindowMs;
    private final boolean enabled;
    
    public SlidingWindowHotKeyDetector(HotKeyDetectionConfig config) {
        this.accessRecords = new ConcurrentHashMap<>();
        this.hotKeys = ConcurrentHashMap.newKeySet();
        this.threshold = config.threshold();
        this.timeWindowMs = config.timeWindow().toMillis();
        this.enabled = config.enabled();
        
        logger.info("SlidingWindowHotKeyDetector initialized with threshold={}, timeWindow={}ms, enabled={}", 
                    threshold, timeWindowMs, enabled);
    }
    
    @Override
    public void recordAccess(String key) {
        if (!enabled) {
            return;
        }
        
        long now = System.currentTimeMillis();
        
        AccessRecord record = accessRecords.computeIfAbsent(key, k -> new AccessRecord());
        record.addAccess(now);
        
        // 清理过期的访问记录
        record.cleanExpired(now - timeWindowMs);
        
        // 检查是否达到热点阈值
        int count = record.getCount();
        if (count >= threshold) {
            if (hotKeys.add(key)) {
                logger.info("Hot key detected: key={}, accessCount={}", key, count);
            }
        } else {
            hotKeys.remove(key);
        }
    }
    
    @Override
    public boolean isHotKey(String key) {
        if (!enabled) {
            return false;
        }
        return hotKeys.contains(key);
    }
    
    @Override
    public Set<String> getHotKeys() {
        if (!enabled) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(hotKeys));
    }
    
    @Override
    public void setThreshold(int accessCount, Duration timeWindow) {
        this.threshold = accessCount;
        this.timeWindowMs = timeWindow.toMillis();
        
        logger.info("Hot key threshold updated: threshold={}, timeWindow={}ms", 
                    accessCount, timeWindowMs);
        
        // 重新评估所有key
        reevaluateAllKeys();
    }
    
    private void reevaluateAllKeys() {
        long now = System.currentTimeMillis();
        long cutoff = now - timeWindowMs;
        
        for (Map.Entry<String, AccessRecord> entry : accessRecords.entrySet()) {
            String key = entry.getKey();
            AccessRecord record = entry.getValue();
            
            record.cleanExpired(cutoff);
            int count = record.getCount();
            
            if (count >= threshold) {
                hotKeys.add(key);
            } else {
                hotKeys.remove(key);
            }
        }
    }
    
    /**
     * 访问记录
     */
    private static class AccessRecord {
        private final Deque<Long> timestamps;
        private final AtomicInteger count;
        
        public AccessRecord() {
            this.timestamps = new ConcurrentLinkedDeque<>();
            this.count = new AtomicInteger(0);
        }
        
        public void addAccess(long timestamp) {
            timestamps.addLast(timestamp);
            count.incrementAndGet();
        }
        
        public void cleanExpired(long cutoff) {
            while (!timestamps.isEmpty()) {
                Long first = timestamps.peekFirst();
                if (first != null && first < cutoff) {
                    timestamps.pollFirst();
                    count.decrementAndGet();
                } else {
                    break;
                }
            }
        }
        
        public int getCount() {
            return count.get();
        }
    }
}
