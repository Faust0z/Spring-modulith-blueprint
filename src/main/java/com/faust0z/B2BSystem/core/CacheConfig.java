package com.faust0z.B2BSystem.core;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
class CacheConfig implements CachingConfigurer {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager() {
            @Override
            @NonNull
            protected CaffeineCache createCaffeineCache(@NonNull String name) {
                switch (name) {
                    case "dashboard" -> {
                        log.debug("Creating custom 5-minute cache for: {}", name);
                        return new CaffeineCache(name, Caffeine.newBuilder()
                                .initialCapacity(10)
                                .maximumSize(50)
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .build(), true);
                    }
                    case "revocations" -> {
                        log.debug("Creating custom 24-hours cache for: {}", name);
                        return new CaffeineCache(name, Caffeine.newBuilder()
                                .initialCapacity(10)
                                .maximumSize(1000)
                                .expireAfterWrite(24, TimeUnit.HOURS)
                                .build(), true);
                    }
                    case "user_dnis" -> {
                        log.debug("Creating custom 12-hour cache for user DNI verification");
                        return new CaffeineCache(name, Caffeine.newBuilder()
                                .initialCapacity(20)
                                .maximumSize(500)
                                .expireAfterWrite(12, TimeUnit.HOURS)
                                .build(), true);
                    }
                }

                log.debug("Creating default 30-minute cache for: {}", name);
                return new CaffeineCache(name, Caffeine.newBuilder()
                        .initialCapacity(20)
                        .maximumSize(150)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .build(), true);
            }
        };

        log.info("CaffeineCacheManager initialized. Dashboard TTL: 5m, Revocations TTL: 24hs, User Dni TTL: 12hs, Default TTL: 30m.");
        return cacheManager;
    }

    @Bean
    com.github.benmanes.caffeine.cache.Cache<String, Bucket> rateLimitBuckets() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("Error GET en caché [{}], clave [{}]: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("Error PUT en caché [{}], clave [{}]: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("Error EVICT en caché [{}], clave [{}]: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("Error CLEAR en caché [{}]: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}