package com.cache.core;

/**
 * 缓存错误类型枚举
 */
public enum CacheErrorType {
    /** 本地缓存错误 */
    L1_ERROR,
    /** 分布式缓存连接错误 */
    L2_CONNECTION_ERROR,
    /** 分布式缓存超时 */
    L2_TIMEOUT,
    /** 数据库错误 */
    DATABASE_ERROR,
    /** 锁超时 */
    LOCK_TIMEOUT,
    /** 序列化错误 */
    SERIALIZATION_ERROR
}
