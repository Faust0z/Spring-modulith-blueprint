package com.faust0z.B2BSystem.auth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import com.faust0z.B2BSystem.auth.PortalJwtAuthenticationConverter;
public class PortalJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final GrantedAuthority PORTAL_AUTHORITY = new SimpleGrantedAuthority("ROLE_PORTAL_CLIENT");

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (!"portal".equals(jwt.getClaimAsString("token_type")))
            throw new InsufficientAuthenticationException("Token is not a portal token");

        return new JwtAuthenticationToken(jwt, List.of(PORTAL_AUTHORITY), jwt.getSubject());
    }
}
