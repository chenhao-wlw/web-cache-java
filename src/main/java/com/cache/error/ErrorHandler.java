package com.cache.error;

/**
 * 错误处理器接口
 */
public interface ErrorHandler {
    
    /**
     * 处理缓存错误
     */
    CacheErrorResponse handle(CacheError error);
}
