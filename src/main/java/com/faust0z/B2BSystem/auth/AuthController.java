package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.core.ClientIpResolver;
import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.user.UserUpdatePinRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

import com.faust0z.B2BSystem.auth.UserAuthResponse;
import com.faust0z.B2BSystem.auth.UserLoginInternalResponse;
import com.faust0z.B2BSystem.auth.AuthService;
import com.faust0z.B2BSystem.auth.AuthController;
import com.faust0z.B2BSystem.auth.UserLoginRequest;
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
@Tag(name = "Auth", description = "Authentication management")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

    @Operation(summary = "Login a user via sending a JWT to their HTTPOnly Cookies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant logged in successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Incorrect email or password")
    })
    @PostMapping("/login")
    public UserLoginInternalResponse login(@Valid @RequestBody UserLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String clientIp = clientIpResolver.resolve(httpRequest);

        rateLimiterService.checkLimit("login:ip:" + clientIp, 10, 2, Duration.ofMinutes(1));
        rateLimiterService.checkLimit("login:identifier:" + request.identifier(), 5, 1, Duration.ofMinutes(5));

        UserAuthResponse authResult = authService.login(request);

        rateLimiterService.resetLimit("login:identifier:" + request.identifier());

        ResponseCookie jwtCookie = ResponseCookie.from("access_token", authResult.token())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(86400)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        return authResult.response();
    }

    @Operation(summary = "Authorize high-privilege actions via PIN")
    @ApiResponse(responseCode = "204", description = "Autorización temporal concedida")
    @PostMapping("/pin-authorization")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void authorizePin(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserUpdatePinRequest request, HttpServletResponse response) {
        UUID userId = UUID.fromString(jwt.getSubject());
        rateLimiterService.checkLimit("pin-auth-verify:" + userId, 5, 1, Duration.ofMinutes(15));

        String token = authService.authorizeAdminPin(userId, request.adminPin());

        ResponseCookie cookie = ResponseCookie.from("pin_auth_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(900)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Operation(summary = "Logout a user by invalidating their HTTPOnly cookie")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada correctamente")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        ResponseCookie jwtCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        ResponseCookie pinCookie = ResponseCookie.from("pin_auth_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, pinCookie.toString());
    }
}
