package com.cache.bloom;

import com.cache.config.BloomFilterConfig;
import com.cache.core.BloomFilterStats;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于Guava的布隆过滤器服务实现
 */
public class GuavaBloomFilterService implements BloomFilterService {
    
    private static final Logger logger = LoggerFactory.getLogger(GuavaBloomFilterService.class);
    
    private final BloomFilterConfig config;
    private final ReadWriteLock lock;
    private final AtomicLong insertionCount;
    
    private volatile BloomFilter<CharSequence> bloomFilter;
    
    public GuavaBloomFilterService(BloomFilterConfig config) {
        this.config = config;
        this.lock = new ReentrantReadWriteLock();
        this.insertionCount = new AtomicLong(0);
        this.bloomFilter = createBloomFilter();
        
        logger.info("GuavaBloomFilterService initialized with expectedInsertions={}, fpp={}", 
                    config.expectedInsertions(), config.falsePositiveRate());
    }
    
    @Override
    public boolean mightContain(String key) {
        lock.readLock().lock();
        try {
            boolean result = bloomFilter.mightContain(key);
            logger.debug("BloomFilter mightContain: key={}, result={}", key, result);
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void add(String key) {
        lock.writeLock().lock();
        try {
            boolean added = bloomFilter.put(key);
            if (added) {
                insertionCount.incrementAndGet();
                logger.debug("BloomFilter add: key={}", key);
                
                // 检查是否需要重建
                checkAndRebuildIfNeeded();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void addAll(Collection<String> keys) {
        lock.writeLock().lock();
        try {
            for (String key : keys) {
                if (bloomFilter.put(key)) {
                    insertionCount.incrementAndGet();
                }
            }
            logger.debug("BloomFilter addAll: count={}", keys.size());
            
            // 检查是否需要重建
            checkAndRebuildIfNeeded();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void rebuild(Collection<String> keys) {
        lock.writeLock().lock();
        try {
            logger.info("Rebuilding BloomFilter with {} keys", keys.size());
            
            // 创建新的布隆过滤器
            BloomFilter<CharSequence> newFilter = createBloomFilter();
            
            // 添加所有key
            for (String key : keys) {
                newFilter.put(key);
            }
            
            // 替换旧的过滤器
            this.bloomFilter = newFilter;
            this.insertionCount.set(keys.size());
            
            logger.info("BloomFilter rebuilt successfully");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public double estimatedFalsePositiveRate() {
        lock.readLock().lock();
        try {
            return bloomFilter.expectedFpp();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public BloomFilterStats stats() {
        lock.readLock().lock();
        try {
            return new BloomFilterStats(
                config.expectedInsertions(),
                insertionCount.get(),
                bloomFilter.expectedFpp(),
                calculateBitSize(),
                calculateHashFunctions()
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private BloomFilter<CharSequence> createBloomFilter() {
        return BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            config.expectedInsertions(),
            config.falsePositiveRate()
        );
    }
    
    private void checkAndRebuildIfNeeded() {
        double currentFpp = bloomFilter.expectedFpp();
        if (currentFpp > config.rebuildThreshold()) {
            logger.warn("BloomFilter FPP {} exceeds threshold {}, consider rebuilding", 
                       currentFpp, config.rebuildThreshold());
        }
    }
    
    private long calculateBitSize() {
        // 估算位数组大小: m = -n * ln(p) / (ln(2)^2)
        double n = config.expectedInsertions();
        double p = config.falsePositiveRate();
        return (long) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }
    
    private int calculateHashFunctions() {
        // 估算哈希函数数量: k = (m/n) * ln(2)
        long m = calculateBitSize();
        double n = config.expectedInsertions();
        return (int) Math.ceil((m / n) * Math.log(2));
    }
}
