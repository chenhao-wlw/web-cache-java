package com.cache.example;

import com.cache.bloom.GuavaBloomFilterService;
import com.cache.circuitbreaker.DefaultCircuitBreaker;
import com.cache.config.*;
import com.cache.core.CacheEntry;
import com.cache.core.CacheLevel;
import com.cache.facade.MultiLevelCacheFacade;
import com.cache.hotkey.SlidingWindowHotKeyDetector;
import com.cache.local.CaffeineLocalCache;
import com.cache.metrics.DefaultCacheMetrics;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 多级缓存系统演示程序
 * 
 * 注意：此示例使用模拟的分布式缓存，无需Redis即可运行
 */
public class CacheDemo {
    
    public static void main(String[] args) {
        System.out.println("=== 多级缓存系统演示 ===\n");
        
        // 1. 创建配置
        CacheConfig config = CacheConfig.builder()
            .localCache(LocalCacheConfig.builder()
                .maxSize(1000)
                .defaultTtl(Duration.ofSeconds(30))
                .recordStats(true)
                .build())
            .bloomFilter(BloomFilterConfig.builder()
                .expectedInsertions(10000)
                .falsePositiveRate(0.01)
                .build())
            .nullCache(NullCacheConfig.builder()
                .enabled(true)
                .ttl(Duration.ofMinutes(1))
                .build())
            .hotKeyDetection(HotKeyDetectionConfig.builder()
                .enabled(true)
                .threshold(5)
                .timeWindow(Duration.ofSeconds(10))
                .build())
            .circuitBreaker(CircuitBreakerConfig.builder()
                .enabled(true)
                .failureThreshold(3)
                .resetTimeout(Duration.ofSeconds(10))
                .build())
            .build();
        
        // 2. 创建组件
        var localCache = new CaffeineLocalCache<String, CacheEntry<String>>(config.localCache());
        var mockDistributedCache = new MockDistributedCache<String, CacheEntry<String>>();
        var bloomFilter = new GuavaBloomFilterService(config.bloomFilter());
        var hotKeyDetector = new SlidingWindowHotKeyDetector(config.hotKeyDetection());
        var circuitBreaker = new DefaultCircuitBreaker(config.circuitBreaker());
        var metrics = new DefaultCacheMetrics();
        
        // 模拟数据库
        Map<String, String> database = new HashMap<>();
        database.put("user:1", "张三");
        database.put("user:2", "李四");
        database.put("user:3", "王五");
        database.put("product:100", "iPhone 15");
        database.put("product:200", "MacBook Pro");
        
        // 预热布隆过滤器
        database.keySet().forEach(bloomFilter::add);
        
        // 3. 创建缓存门面
        var cacheFacade = new MultiLevelCacheFacade<>(
            localCache,
            mockDistributedCache,
            bloomFilter,
            hotKeyDetector,
            circuitBreaker,
            metrics,
            config,
            key -> {
                System.out.println("  [数据库] 加载数据: " + key);
                return database.get(key);
            }
        );
        
        // 4. 演示缓存操作
        System.out.println("--- 测试1: 基本缓存操作 ---");
        
        // 第一次查询 - 缓存未命中，从数据库加载
        System.out.println("\n查询 user:1 (首次):");
        Optional<String> result1 = cacheFacade.get("user:1");
        System.out.println("  结果: " + result1.orElse("null"));
        
        // 第二次查询 - 缓存命中
        System.out.println("\n查询 user:1 (第二次，应该命中缓存):");
        Optional<String> result2 = cacheFacade.get("user:1");
        System.out.println("  结果: " + result2.orElse("null"));
        
        System.out.println("\n--- 测试2: 缓存穿透防护 ---");
        
        // 查询不存在的key - 布隆过滤器拦截
        System.out.println("\n查询 user:999 (不存在，布隆过滤器拦截):");
        Optional<String> result3 = cacheFacade.get("user:999");
        System.out.println("  结果: " + result3.orElse("null") + " (被布隆过滤器拦截，未查询数据库)");
        
        System.out.println("\n--- 测试3: 热点Key检测 ---");
        
        // 多次访问同一个key，触发热点检测
        System.out.println("\n连续访问 product:100 (触发热点检测):");
        for (int i = 0; i < 6; i++) {
            cacheFacade.get("product:100");
        }
        System.out.println("  product:100 是否为热点Key: " + hotKeyDetector.isHotKey("product:100"));
        System.out.println("  当前热点Key列表: " + hotKeyDetector.getHotKeys());
        
        System.out.println("\n--- 测试4: 缓存失效（延迟双删）---");
        
        System.out.println("\n失效 user:1 缓存:");
        cacheFacade.invalidate("user:1");
        System.out.println("  已触发延迟双删策略");
        
        // 等待双删完成
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n再次查询 user:1 (应该重新从数据库加载):");
        Optional<String> result4 = cacheFacade.get("user:1");
        System.out.println("  结果: " + result4.orElse("null"));
        
        System.out.println("\n--- 测试5: 监控指标 ---");
        
        var snapshot = metrics.snapshot();
        System.out.println("\n缓存统计:");
        System.out.println("  L1命中次数: " + snapshot.hitCounts().get(CacheLevel.L1_LOCAL));
        System.out.println("  L1未命中次数: " + snapshot.missCounts().get(CacheLevel.L1_LOCAL));
        System.out.println("  L1命中率: " + String.format("%.2f%%", snapshot.hitRates().get(CacheLevel.L1_LOCAL) * 100));
        
        System.out.println("\n--- 测试6: Prometheus指标导出 ---");
        
        System.out.println("\nPrometheus格式指标:");
        System.out.println(metrics.exportPrometheus());
        
        // 清理资源
        cacheFacade.close();
        
        System.out.println("=== 演示完成 ===");
    }
}
