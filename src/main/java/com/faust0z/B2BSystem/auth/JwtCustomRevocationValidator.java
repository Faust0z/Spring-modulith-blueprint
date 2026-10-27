package com.faust0z.B2BSystem.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import com.faust0z.B2BSystem.auth.JwtCustomRevocationValidator;
@RequiredArgsConstructor
public class JwtCustomRevocationValidator implements OAuth2TokenValidator<Jwt> {

    private final CacheManager cacheManager;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Instant iat = jwt.getIssuedAt();
        if (iat == null) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        }

        Cache cache = cacheManager.getCache("revocations");
        if (cache == null) return OAuth2TokenValidatorResult.success();

        Long userRevokedAt = cache.get("user:" + jwt.getSubject(), Long.class);
        if (userRevokedAt != null && iat.getEpochSecond() < userRevokedAt) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("token_revoked"));
        }

        String tenantId = jwt.getClaimAsString("tenantId");
        if (tenantId != null) {
            Long tenantRevokedAt = cache.get("tenant:" + tenantId, Long.class);
            if (tenantRevokedAt != null && iat.getEpochSecond() < tenantRevokedAt) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("token_revoked"));
            }
        }

        return OAuth2TokenValidatorResult.success();
    }
}