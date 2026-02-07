package com.cache.facade;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 缓存门面接口
 */
public interface CacheFacade<K, V> {
    
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
     * 批量获取
     */
    Map<K, V> multiGet(Collection<K> keys);
    
    /**
     * 批量设置
     */
    void multiPut(Map<K, V> entries, Duration ttl);
    
    /**
     * 使缓存失效（延迟双删）
     */
    void invalidate(K key);
    
    /**
     * 关闭资源
     */
    void close();
}
