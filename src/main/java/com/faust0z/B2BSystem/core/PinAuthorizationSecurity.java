package com.faust0z.B2BSystem.core;

import com.faust0z.B2BSystem.user.UserInternalApi;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;

@Component("pinAuthorizationSecurity")
@RequiredArgsConstructor
public class PinAuthorizationSecurity {
    private final JwtDecoder jwtDecoder;
    private final UserInternalApi userInternalApi;

    public boolean isElevated() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();

        boolean hasValidToken = false;

        if (request.getCookies() != null) {
            hasValidToken = Arrays.stream(request.getCookies())
                    .filter(c -> "pin_auth_token".equals(c.getName()))
                    .findFirst()
                    .map(cookie -> {
                        try {
                            Jwt jwt = jwtDecoder.decode(cookie.getValue());
                            return "pin-auth".equals(jwt.getClaimAsString("token_type"));
                        } catch (JwtException e) {
                            return false;
                        }
                    })
                    .orElse(false);
        }
        if (!hasValidToken) throw new PinRequiredException();

        return true;
    }

    public boolean isElevatedOrInitialSetup(String userIdStr) {
        if (!userInternalApi.existsByIdAndAdminPinNotNull(UUID.fromString(userIdStr))) return true;
        return isElevated();
    }
}