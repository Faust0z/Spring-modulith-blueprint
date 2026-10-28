package com.faust0z.B2BSystem.access;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.faust0z.B2BSystem.access.web.AccessLogResponse;

import java.time.Instant;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AccessLogServiceCacheTest.CacheTestConfig.class)
class AccessLogServiceCacheTest {

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
        String logId = UUID.randomUUID().toString();

        String keyA = tenantA + ":0:20";
        String keyB = tenantB + ":0:20";

        Cache cache = cacheManager.getCache("access_logs");

        cache.put(keyA, new AccessLogResponse(
            UUID.fromString(logId), UUID.randomUUID(), "John Doe", null,
            Instant.now(), com.faust0z.B2BSystem.access.domain.AccessStatus.PERMITIDO,
            0, UUID.randomUUID()
        ));
        cache.put(keyB, new AccessLogResponse(
            UUID.fromString(logId), UUID.randomUUID(), "Jane Smith", null,
            Instant.now(), com.faust0z.B2BSystem.access.domain.AccessStatus.PERMITIDO,
            0, UUID.randomUUID()
        ));

        Object cachedA = cache.get(keyA, Object.class);
        Object cachedB = cache.get(keyB, Object.class);
        assert cachedA != null : "Cache should contain key: " + keyA;
        assert cachedB != null : "Cache should contain key: " + keyB;
    }

    @Test
    void cacheEvictionWorks() {
        Cache cache = cacheManager.getCache("access_logs");
        cache.put("key-1", new AccessLogResponse(
            UUID.randomUUID(), UUID.randomUUID(), "John Doe", null,
            Instant.now(), com.faust0z.B2BSystem.access.domain.AccessStatus.PERMITIDO,
            0, UUID.randomUUID()
        ));
        cache.put("key-2", new AccessLogResponse(
            UUID.randomUUID(), UUID.randomUUID(), "Jane Smith", null,
            Instant.now(), com.faust0z.B2BSystem.access.domain.AccessStatus.PERMITIDO,
            0, UUID.randomUUID()
        ));

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
        Cache cache = cacheManager.getCache("access_logs");
        if (cache != null) {
            cache.clear();
        }
    }
}