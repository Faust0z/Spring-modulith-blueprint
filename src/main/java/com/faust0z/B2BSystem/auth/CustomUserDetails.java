package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserAuthInternalResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.faust0z.B2BSystem.auth.CustomUserDetails;
public class CustomUserDetails implements UserDetails {
    private final UserAuthInternalResponse userAuthResponse;

    public CustomUserDetails(UserAuthInternalResponse userDTO) {
        this.userAuthResponse = userDTO;
    }

    public UUID getId() {
        return userAuthResponse.id();
    }

    public UUID getTenantId() {
        return userAuthResponse.tenantId();
    }

    public Role getRole() {
        return userAuthResponse.role();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userAuthResponse.role().name()));
    }

    @Override
    public String getPassword() {
        return userAuthResponse.password();
    }

    @Override
    public String getUsername() {
        return userAuthResponse.email();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}