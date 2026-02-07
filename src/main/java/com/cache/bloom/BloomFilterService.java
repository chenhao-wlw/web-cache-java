package com.cache.bloom;

import com.cache.core.BloomFilterStats;

import java.util.Collection;

/**
 * 布隆过滤器服务接口
 */
public interface BloomFilterService {
    
    /**
     * 检查key是否可能存在
     */
    boolean mightContain(String key);
    
    /**
     * 添加key到过滤器
     */
    void add(String key);
    
    /**
     * 批量添加
     */
    void addAll(Collection<String> keys);
    
    /**
     * 重建过滤器
     */
    void rebuild(Collection<String> keys);
    
    /**
     * 获取当前误判率估计
     */
    double estimatedFalsePositiveRate();
    
    /**
     * 获取过滤器统计信息
     */
    BloomFilterStats stats();
}
