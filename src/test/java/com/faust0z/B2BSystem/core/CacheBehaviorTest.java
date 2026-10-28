package com.faust0z.B2BSystem.core;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CacheBehaviorTest.CacheTestConfig.class)
class CacheBehaviorTest {

    @Configuration
    @org.springframework.cache.annotation.EnableCaching
    static class CacheTestConfig implements CachingConfigurer {
        @Bean
        @Override
        public CacheManager cacheManager() {
            return new CaffeineCacheManager();
        }
    }

    @Autowired
    private CacheManager cacheManager;

    @Test
    void tenantScopedCacheKeysAreIsolated() {
        String tenantA = "tenant-a";
        String tenantB = "tenant-b";
        String key = "some-key";
        String valueA = "value-a";
        String valueB = "value-b";

        String keyA = tenantA + ":" + key;
        String keyB = tenantB + ":" + key;

        org.springframework.cache.Cache cache = cacheManager.getCache("test-cache");

        cache.put(keyA, valueA);
        cache.put(keyB, valueB);

        Object cachedA = cache.get(keyA, Object.class);
        Object cachedB = cache.get(keyB, Object.class);
        assert cachedA != null : "Cache should contain key: " + keyA;
        assert cachedB != null : "Cache should contain key: " + keyB;

        assert cachedA.equals(valueA) : "Cache value mismatch for key: " + keyA;
        assert cachedB.equals(valueB) : "Cache value mismatch for key: " + keyB;
    }

    @Test
    void cacheEvictionWorks() {
        org.springframework.cache.Cache cache = cacheManager.getCache("test-cache");
        cache.put("key-1", "value-1");
        cache.put("key-2", "value-2");

        Object v1 = cache.get("key-1", Object.class);
        Object v2 = cache.get("key-2", Object.class);
        assert v1 != null : "Cache should contain key-1";
        assert v2 != null : "Cache should contain key-2";

        cache.evict("key-1");
        Object v1After = cache.get("key-1", Object.class);
        assert v1After == null : "Evicted key should return null";
    }

    @AfterEach
    void tearDown() {
        org.springframework.cache.Cache cache = cacheManager.getCache("test-cache");
        if (cache != null) {
            cache.clear();
        }
    }
}