package com.cache.core;

/**
 * 缓存Key解析结果
 */
public record CacheKeyParts(String namespace, String key) {
}
