package com.faust0z.B2BSystem.core;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;

@Import(AbstractJpaSliceTest.TestCacheConfig.class)
abstract class AbstractJpaSliceTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg18")
        .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.cache.type", () -> "caffeine");
    }

    @Configuration
    static class TestCacheConfig {
        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager();
        }
    }

    @org.junit.jupiter.api.AfterEach
    void clearContext() {
        TenantContext.clear();
    }
}