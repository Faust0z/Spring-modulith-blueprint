package com.faust0z.B2BSystem.auth;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.faust0z.B2BSystem.auth.JwtCustomRevocationValidator;
class JwtCustomRevocationValidatorTest {

    @Test
    void validate_NullIssuedAt_ReturnsFailure() {
        CacheManager cacheManager = mock(CacheManager.class);
        JwtCustomRevocationValidator validator = new JwtCustomRevocationValidator(cacheManager);
        Jwt jwt = mock(Jwt.class);
        given(jwt.getIssuedAt()).willReturn(null);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
    }

    @Test
    void validate_CacheMissing_ReturnsSuccess() {
        CacheManager cacheManager = mock(CacheManager.class);
        JwtCustomRevocationValidator validator = new JwtCustomRevocationValidator(cacheManager);
        Jwt jwt = mock(Jwt.class);
        given(jwt.getIssuedAt()).willReturn(Instant.now());
        given(cacheManager.getCache("revocations")).willReturn(null);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_UserRevokedAfterIssuedAt_ReturnsFailure() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        JwtCustomRevocationValidator validator = new JwtCustomRevocationValidator(cacheManager);
        Jwt jwt = mock(Jwt.class);
        Instant iat = Instant.ofEpochSecond(1000);

        given(jwt.getIssuedAt()).willReturn(iat);
        given(jwt.getSubject()).willReturn("user-123");
        given(cacheManager.getCache("revocations")).willReturn(cache);
        given(cache.get("user:user-123", Long.class)).willReturn(1005L);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("token_revoked");
    }

    @Test
    void validate_TenantRevokedAfterIssuedAt_ReturnsFailure() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        JwtCustomRevocationValidator validator = new JwtCustomRevocationValidator(cacheManager);
        Jwt jwt = mock(Jwt.class);
        Instant iat = Instant.ofEpochSecond(1000);

        given(jwt.getIssuedAt()).willReturn(iat);
        given(jwt.getSubject()).willReturn("user-123");
        given(jwt.getClaimAsString("tenantId")).willReturn("tenant-456");
        given(cacheManager.getCache("revocations")).willReturn(cache);
        given(cache.get("user:user-123", Long.class)).willReturn(null);
        given(cache.get("tenant:tenant-456", Long.class)).willReturn(1005L);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("token_revoked");
    }

    @Test
    void validate_NoRevocationsMatch_ReturnsSuccess() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        JwtCustomRevocationValidator validator = new JwtCustomRevocationValidator(cacheManager);
        Jwt jwt = mock(Jwt.class);
        Instant iat = Instant.ofEpochSecond(1000);

        given(jwt.getIssuedAt()).willReturn(iat);
        given(jwt.getSubject()).willReturn("user-123");
        given(jwt.getClaimAsString("tenantId")).willReturn("tenant-456");
        given(cacheManager.getCache("revocations")).willReturn(cache);
        given(cache.get("user:user-123", Long.class)).willReturn(995L);
        given(cache.get("tenant:tenant-456", Long.class)).willReturn(990L);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }
}