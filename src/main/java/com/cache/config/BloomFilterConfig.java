package com.cache.config;

/**
 * 布隆过滤器配置
 */
public record BloomFilterConfig(
    long expectedInsertions,
    double falsePositiveRate,
    double rebuildThreshold
) {
    public static BloomFilterConfig defaults() {
        return new BloomFilterConfig(100000, 0.01, 0.05);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private long expectedInsertions = 100000;
        private double falsePositiveRate = 0.01;
        private double rebuildThreshold = 0.05;
        
        public Builder expectedInsertions(long expectedInsertions) {
            this.expectedInsertions = expectedInsertions;
            return this;
        }
        
        public Builder falsePositiveRate(double falsePositiveRate) {
            if (falsePositiveRate <= 0 || falsePositiveRate >= 1) {
                throw new IllegalArgumentException("False positive rate must be between 0 and 1");
            }
            this.falsePositiveRate = falsePositiveRate;
            return this;
        }
        
        public Builder rebuildThreshold(double rebuildThreshold) {
            this.rebuildThreshold = rebuildThreshold;
            return this;
        }
        
        public BloomFilterConfig build() {
            return new BloomFilterConfig(expectedInsertions, falsePositiveRate, rebuildThreshold);
        }
    }
}
