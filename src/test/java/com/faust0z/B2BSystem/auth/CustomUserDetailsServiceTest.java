package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserAuthInternalResponse;
import com.faust0z.B2BSystem.user.UserInternalApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.faust0z.B2BSystem.auth.CustomUserDetailsService;
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserInternalApi userInternalApi;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @ParameterizedTest
    @ValueSource(strings = {"tenant@test.com", "TENANT@TEST.COM", "tenantuser", "TENANTUSER"})
    void loadUserByUsername_ValidIdentifier_ReturnsUserDetails(String identifier) {
        String normalizedIdentifier = identifier.toLowerCase();
        UserAuthInternalResponse response = new UserAuthInternalResponse(
                UUID.randomUUID(), UUID.randomUUID(), normalizedIdentifier, "hashedPassword", Role.TENANT_ADMIN, true
        );
        given(userInternalApi.getUserAuthInternalByIdentifier(normalizedIdentifier)).willReturn(response);

        UserDetails result = userDetailsService.loadUserByUsername(identifier);

        assertThat(result.getUsername()).isEqualTo(normalizedIdentifier);
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_TENANT_ADMIN");
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsUsernameNotFoundException() {
        String identifier = "missing@test.com";
        given(userInternalApi.getUserAuthInternalByIdentifier(identifier)).willThrow(new RuntimeException("Not found"));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(identifier))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(identifier);
    }
}