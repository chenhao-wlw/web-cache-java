package com.cache.distributed;

import com.cache.config.DistributedCacheConfig;
import com.cache.core.LockResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于Redis + Lettuce的分布式缓存实现
 */
public class RedisDistributedCache<K, V> implements DistributedCache<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisDistributedCache.class);
    private static final String LOCK_PREFIX = "lock:";
    
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;
    private final ObjectMapper objectMapper;
    private final DistributedCacheConfig config;
    private final Class<V> valueType;
    
    public RedisDistributedCache(DistributedCacheConfig config, Class<V> valueType) {
        this.config = config;
        this.valueType = valueType;
        this.redisClient = RedisClient.create(config.redisUri());
        this.connection = redisClient.connect();
        this.commands = connection.sync();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        logger.info("RedisDistributedCache initialized with uri={}", config.redisUri());
    }
    
    @Override
    public Optional<V> get(K key) {
        try {
            String keyStr = serializeKey(key);
            String value = commands.get(keyStr);
            
            if (value != null) {
                logger.debug("Redis cache hit: key={}", keyStr);
                return Optional.of(deserializeValue(value));
            }
            
            logger.debug("Redis cache miss: key={}", keyStr);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Redis get error: key={}", key, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        try {
            String keyStr = serializeKey(key);
            String valueStr = serializeValue(value);
            Duration effectiveTtl = ttl != null ? ttl : config.defaultTtl();
            
            commands.setex(keyStr, effectiveTtl.getSeconds(), valueStr);
            logger.debug("Redis cache put: key={}, ttl={}", keyStr, effectiveTtl);
        } catch (Exception e) {
            logger.error("Redis put error: key={}", key, e);
        }
    }
    
    @Override
    public void delete(K key) {
        try {
            String keyStr = serializeKey(key);
            commands.del(keyStr);
            logger.debug("Redis cache delete: key={}", keyStr);
        } catch (Exception e) {
            logger.error("Redis delete error: key={}", key, e);
        }
    }
    
    @Override
    public LockResult<V> getWithLock(K key, Duration lockTimeout) {
        String keyStr = serializeKey(key);
        String lockKey = LOCK_PREFIX + keyStr;
        String lockToken = UUID.randomUUID().toString();
        
        try {
            // 尝试获取锁
            String result = commands.set(lockKey, lockToken, 
                SetArgs.Builder.nx().ex(lockTimeout.getSeconds()));
            
            if ("OK".equals(result)) {
                // 获取锁成功
                Optional<V> value = get(key);
                logger.debug("Lock acquired: key={}, token={}", keyStr, lockToken);
                return LockResult.acquired(value.orElse(null), lockToken);
            } else {
                // 获取锁失败，返回当前缓存值
                Optional<V> value = get(key);
                logger.debug("Lock not acquired: key={}", keyStr);
                return LockResult.notAcquired(value.orElse(null));
            }
        } catch (Exception e) {
            logger.error("Redis getWithLock error: key={}", keyStr, e);
            return LockResult.notAcquired();
        }
    }
    
    @Override
    public void releaseLock(K key, String lockToken) {
        if (lockToken == null) {
            return;
        }
        
        String keyStr = serializeKey(key);
        String lockKey = LOCK_PREFIX + keyStr;
        
        try {
            // 使用Lua脚本确保只释放自己的锁
            String script = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;
            
            commands.eval(script, io.lettuce.core.ScriptOutputType.INTEGER, 
                         new String[]{lockKey}, lockToken);
            logger.debug("Lock released: key={}, token={}", keyStr, lockToken);
        } catch (Exception e) {
            logger.error("Redis releaseLock error: key={}", keyStr, e);
        }
    }
    
    @Override
    public void putWithRandomTtl(K key, V value, Duration baseTtl, int jitterPercent) {
        // 计算随机TTL偏移量 (10% - 30%)
        int effectiveJitter = Math.max(10, Math.min(30, jitterPercent));
        double jitterFactor = ThreadLocalRandom.current().nextDouble(0.10, 0.30);
        
        // 随机决定增加或减少
        boolean increase = ThreadLocalRandom.current().nextBoolean();
        long baseTtlSeconds = baseTtl.getSeconds();
        long jitterSeconds = (long) (baseTtlSeconds * jitterFactor);
        
        long actualTtlSeconds = increase ? 
            baseTtlSeconds + jitterSeconds : 
            baseTtlSeconds - jitterSeconds;
        
        // 确保TTL至少为1秒
        actualTtlSeconds = Math.max(1, actualTtlSeconds);
        
        Duration actualTtl = Duration.ofSeconds(actualTtlSeconds);
        put(key, value, actualTtl);
        
        logger.debug("Redis cache put with jitter: key={}, baseTtl={}, actualTtl={}", 
                    serializeKey(key), baseTtl, actualTtl);
    }
    
    @Override
    public void close() {
        try {
            connection.close();
            redisClient.shutdown();
            logger.info("RedisDistributedCache closed");
        } catch (Exception e) {
            logger.error("Error closing Redis connection", e);
        }
    }
    
    private String serializeKey(K key) {
        return key.toString();
    }
    
    private String serializeValue(V value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize value", e);
        }
    }
    
    private V deserializeValue(String value) {
        try {
            return objectMapper.readValue(value, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize value", e);
        }
    }
}
