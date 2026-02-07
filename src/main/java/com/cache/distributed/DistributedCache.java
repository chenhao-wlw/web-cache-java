package com.cache.distributed;

import com.cache.core.LockResult;

import java.time.Duration;
import java.util.Optional;

/**
 * 分布式缓存接口
 */
public interface DistributedCache<K, V> {
    
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
     * 带锁的获取（击穿防护）
     */
    LockResult<V> getWithLock(K key, Duration lockTimeout);
    
    /**
     * 释放锁
     */
    void releaseLock(K key, String lockToken);
    
    /**
     * 设置带随机TTL（雪崩防护）
     */
    void putWithRandomTtl(K key, V value, Duration baseTtl, int jitterPercent);
    
    /**
     * 关闭连接
     */
    void close();
}
