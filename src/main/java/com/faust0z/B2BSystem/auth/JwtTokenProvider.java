package com.faust0z.B2BSystem.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.faust0z.B2BSystem.auth.CustomUserDetails;
import com.faust0z.B2BSystem.auth.JwtTokenProvider;
@Slf4j
@Service
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final long jwtExpirationMs;

    public JwtTokenProvider(JwtEncoder jwtEncoder, @Value("${app.jwt.expiration-ms}") long jwtExpirationMs) {
        this.jwtEncoder = jwtEncoder;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(UserDetails userDetails) {
        CustomUserDetails user = (CustomUserDetails) userDetails;
        log.debug("Generating JWT token for userId: {}", user.getId());

        Instant now = Instant.now();

        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("B2B-System")
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtExpirationMs))
                .subject(user.getId().toString())
                .claim("authorities", authorities)
                .claim("role", user.getRole().name());

        if (user.getTenantId() != null) {
            claimsBuilder.claim("tenantId", user.getTenantId().toString());
        }

        JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsBuilder.build())).getTokenValue();
    }

    public String generatePortalToken(UUID clientId, UUID tenantId, boolean isOwner, boolean isRenter) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("B2B-System")
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtExpirationMs))
                .subject(clientId.toString())
                .claim("token_type", "portal")
                .claim("tenantId", tenantId.toString())
                .claim("is_owner", isOwner)
                .claim("is_renter", isRenter)
                .claim("authorities", List.of("ROLE_PORTAL_CLIENT"));

        JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsBuilder.build())).getTokenValue();
    }

    public String generatePinAuthorizationToken(UUID userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("B2B-System")
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .subject(userId.toString())
                .claim("token_type", "pin-auth")
                .build();

        JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}