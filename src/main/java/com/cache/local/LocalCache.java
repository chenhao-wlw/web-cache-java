package com.cache.local;

import com.cache.core.CacheStats;

import java.time.Duration;
import java.util.Optional;

/**
 * 本地缓存接口
 */
public interface LocalCache<K, V> {
    
    /**
     * 获取缓存值
     */
    Optional<V> get(K key);
    
    /**
     * 设置缓存值
     */
    void put(K key, V value, Duration ttl);
    
    /**
     * 删除缓存
     */
    void delete(K key);
    
    /**
     * 清空所有缓存
     */
    void clear();
    
    /**
     * 获取缓存大小
     */
    long size();
    
    /**
     * 获取缓存统计信息
     */
    CacheStats stats();
}
