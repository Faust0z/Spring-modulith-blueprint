package com.faust0z.B2BSystem.core;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {
    private final com.github.benmanes.caffeine.cache.Cache<String, Bucket> buckets;

    public RateLimiterService(com.github.benmanes.caffeine.cache.Cache<String, Bucket> buckets) {
        this.buckets = buckets;
    }

    public void checkLimit(String key, int capacity, int refillTokens, Duration period) {
        Bucket bucket = buckets.get(key, k -> Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(refillTokens, period))
                .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long minutesToWait = TimeUnit.NANOSECONDS.toMinutes(probe.getNanosToWaitForRefill()) + 1;
            throw new TooManyRequestsException(minutesToWait, "minutos");
        }
    }

    public void resetLimit(String key) {
        buckets.invalidate(key);
    }
}