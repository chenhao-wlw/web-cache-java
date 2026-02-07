package com.cache.example;

import com.cache.core.LockResult;
import com.cache.distributed.DistributedCache;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟分布式缓存（用于演示，无需Redis）
 */
public class MockDistributedCache<K, V> implements DistributedCache<K, V> {
    
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, String> locks = new ConcurrentHashMap<>();
    
    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(cache.get(key));
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        cache.put(key, value);
    }
    
    @Override
    public void delete(K key) {
        cache.remove(key);
    }
    
    @Override
    public LockResult<V> getWithLock(K key, Duration lockTimeout) {
        String token = UUID.randomUUID().toString();
        String existing = locks.putIfAbsent(key, token);
        
        if (existing == null) {
            return LockResult.acquired(cache.get(key), token);
        } else {
            return LockResult.notAcquired(cache.get(key));
        }
    }
    
    @Override
    public void releaseLock(K key, String lockToken) {
        locks.remove(key, lockToken);
    }
    
    @Override
    public void putWithRandomTtl(K key, V value, Duration baseTtl, int jitterPercent) {
        cache.put(key, value);
    }
    
    @Override
    public void close() {
        cache.clear();
        locks.clear();
    }
}
