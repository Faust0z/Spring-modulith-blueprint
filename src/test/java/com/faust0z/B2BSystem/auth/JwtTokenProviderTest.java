package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.user.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.auth.CustomUserDetails;
import com.faust0z.B2BSystem.auth.JwtTokenProvider;
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private static final long EXPIRATION_MS = 3600000L;

    private JwtTokenProvider createProvider() {
        return new JwtTokenProvider(jwtEncoder, EXPIRATION_MS);
    }

    @Nested
    class GenerateToken {
        @Test
        void generateToken_WithTenantId_IncludesTenantClaim() {
            JwtTokenProvider provider = createProvider();
            CustomUserDetails user = mock(CustomUserDetails.class);
            UUID userId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();

            when(user.getId()).thenReturn(userId);
            when(user.getTenantId()).thenReturn(tenantId);
            when(user.getRole()).thenReturn(Role.TENANT_ADMIN);
            when(user.getAuthorities()).thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")));

            Jwt mockJwt = Jwt.withTokenValue("mock.jwt.token").header("alg", "HS256").claim("sub", userId.toString()).build();
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            String token = provider.generateToken(user);

            ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
            verify(jwtEncoder).encode(captor.capture());
            JwtClaimsSet claims = captor.getValue().getClaims();

            assertThat(token).isEqualTo("mock.jwt.token");
            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.getClaimAsString("tenantId")).isEqualTo(tenantId.toString());
            assertThat(claims.getClaimAsString("role")).isEqualTo("TENANT_ADMIN");
            assertThat(claims.getClaimAsStringList("authorities")).containsExactly("ROLE_TENANT_ADMIN");
            assertThat(claims.getClaimAsString("iss")).isEqualTo("B2B-System");
        }

        @Test
        void generateToken_WithoutTenantId_ExcludesTenantClaim() {
            JwtTokenProvider provider = createProvider();
            CustomUserDetails user = mock(CustomUserDetails.class);
            UUID userId = UUID.randomUUID();

            when(user.getId()).thenReturn(userId);
            when(user.getTenantId()).thenReturn(null);
            when(user.getRole()).thenReturn(Role.ADMIN);
            when(user.getAuthorities()).thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

            Jwt mockJwt = Jwt.withTokenValue("mock.jwt.token").header("alg", "HS256").claim("sub", userId.toString()).build();
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            provider.generateToken(user);

            ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
            verify(jwtEncoder).encode(captor.capture());
            JwtClaimsSet claims = captor.getValue().getClaims();

            assertThat(claims.getClaims()).doesNotContainKey("tenantId");
            assertThat(claims.getSubject()).isEqualTo(userId.toString());
        }
    }

    @Nested
    class GeneratePinAuthorizationToken {
        @Test
        void generatePinAuthorizationToken_ValidUserId_ReturnsTokenWithPinClaims() {
            JwtTokenProvider provider = createProvider();
            UUID userId = UUID.randomUUID();
            Jwt mockJwt = Jwt.withTokenValue("mock.pin.token").header("alg", "HS256").claim("sub", userId.toString()).build();
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            String token = provider.generatePinAuthorizationToken(userId);

            ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
            verify(jwtEncoder).encode(captor.capture());
            JwtClaimsSet claims = captor.getValue().getClaims();

            assertThat(token).isEqualTo("mock.pin.token");
            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.getClaimAsString("token_type")).isEqualTo("pin-auth");
            assertThat(claims.getClaimAsString("iss")).isEqualTo("B2B-System");
            assertThat(claims.getExpiresAt()).isAfter(Instant.now());
        }
    }
}