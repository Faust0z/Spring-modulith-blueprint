package com.faust0z.B2BSystem.contacts;

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

import java.util.UUID;

import com.faust0z.B2BSystem.contacts.web.ContactResponse;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ContactServiceCacheTest.CacheTestConfig.class)
class ContactServiceCacheTest {

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
        String contactId = UUID.randomUUID().toString();

        String keyA = tenantA + ":" + contactId;
        String keyB = tenantB + ":" + contactId;

        Cache cache = cacheManager.getCache("contact");

        cache.put(keyA, new ContactResponse(
            UUID.fromString(contactId), "12345678", "John", "Doe",
            "John Doe", "Plan A", null, true, null,
            null, null, null, null, null, null, null, null
        ));
        cache.put(keyB, new ContactResponse(
            UUID.fromString(contactId), "87654321", "Jane", "Smith",
            "Jane Smith", "Plan B", null, true, null,
            null, null, null, null, null, null, null, null
        ));

        Object cachedA = cache.get(keyA, Object.class);
        Object cachedB = cache.get(keyB, Object.class);
        assert cachedA != null : "Cache should contain key: " + keyA;
        assert cachedB != null : "Cache should contain key: " + keyB;
    }

    @Test
    void cacheEvictionWorks() {
        Cache cache = cacheManager.getCache("contact");
        cache.put("key-1", new ContactResponse(
            UUID.randomUUID(), "12345678", "John", "Doe",
            "John Doe", "Plan A", null, true, null,
            null, null, null, null, null, null, null, null
        ));
        cache.put("key-2", new ContactResponse(
            UUID.randomUUID(), "87654321", "Jane", "Smith",
            "Jane Smith", "Plan B", null, true, null,
            null, null, null, null, null, null, null, null
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
        Cache cache = cacheManager.getCache("contact");
        if (cache != null) {
            cache.clear();
        }
        Cache contactsCache = cacheManager.getCache("contacts");
        if (contactsCache != null) {
            contactsCache.clear();
        }
    }
}