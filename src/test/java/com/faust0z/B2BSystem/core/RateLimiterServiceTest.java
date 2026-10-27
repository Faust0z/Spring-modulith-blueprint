package com.faust0z.B2BSystem.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.core.TooManyRequestsException;
class RateLimiterServiceTest {

    private RateLimiterService createService() {
        Cache<String, Bucket> cache = Caffeine.newBuilder().build();
        return new RateLimiterService(cache);
    }

    @Test
    void checkLimit_WithinCapacity_ExecutesSuccessfully() {
        RateLimiterService service = createService();
        String key = "auth-ip:127.0.0.1";
        int capacity = 2;

        assertThatCode(() -> {
            service.checkLimit(key, capacity, 1, Duration.ofMinutes(1));
            service.checkLimit(key, capacity, 1, Duration.ofMinutes(1));
        }).doesNotThrowAnyException();
    }

    @Test
    void checkLimit_ExceedsCapacity_ThrowsTooManyRequestsException() {
        RateLimiterService service = createService();
        String key = "auth-ip:127.0.0.1";
        int capacity = 1;
        Duration period = Duration.ofMinutes(5);

        service.checkLimit(key, capacity, 1, period);

        assertThatThrownBy(() -> service.checkLimit(key, capacity, 1, period))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void checkLimit_DifferentKeys_TrackedIndependently() {
        RateLimiterService service = createService();
        int capacity = 1;
        Duration period = Duration.ofMinutes(5);

        service.checkLimit("user-a", capacity, 1, period);

        assertThatCode(() -> service.checkLimit("user-b", capacity, 1, period))
                .doesNotThrowAnyException();
    }
}