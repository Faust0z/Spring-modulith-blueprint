package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.core.ClientIpResolver;
import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.user.UserUpdatePinRequest;
import com.faust0z.B2BSystem.user.InvalidAdminPinException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.faust0z.B2BSystem.auth.UserAuthResponse;
import com.faust0z.B2BSystem.auth.UserLoginInternalResponse;
import com.faust0z.B2BSystem.auth.AuthService;
import com.faust0z.B2BSystem.auth.AuthController;
import com.faust0z.B2BSystem.auth.UserLoginRequest;
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Mock
    private RateLimiterService rateLimiterService;

    @Test
    void login_ValidRequest_ChecksRateLimitsSetsCookieAndReturnsResponse() {
        AuthService authService = mock(AuthService.class);
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        AuthController authController = new AuthController(authService, rateLimiterService, clientIpResolver);

        UserLoginRequest request = new UserLoginRequest("admin@test.com", "pass");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        UserAuthResponse authResult = createAuthResult();

        given(clientIpResolver.resolve(httpRequest)).willReturn("192.168.1.100");
        given(authService.login(request)).willReturn(authResult);

        UserLoginInternalResponse result = authController.login(request, httpRequest, httpResponse);

        InOrder inOrder = Mockito.inOrder(rateLimiterService, authService);
        inOrder.verify(rateLimiterService).checkLimit(eq("login:ip:192.168.1.100"), eq(10), eq(2), eq(Duration.ofMinutes(1)));
        inOrder.verify(rateLimiterService).checkLimit(eq("login:identifier:admin@test.com"), eq(5), eq(1), eq(Duration.ofMinutes(5)));
        inOrder.verify(authService).login(request);
        inOrder.verify(rateLimiterService).resetLimit(eq("login:identifier:admin@test.com"));

        assertThat(result).isEqualTo(authResult.response());
        assertThat(httpResponse.getHeader(HttpHeaders.SET_COOKIE))
                .contains("access_token=" + authResult.token())
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=None")
                .contains("Max-Age=86400");
    }

    @Test
    void logout_ValidRequest_ClearsCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authController.logout(response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("access_token=")
                .contains("Max-Age=0")
                .contains("HttpOnly");
    }

    @Test
    void authorizePin_ValidPin_SetsPinCookie() {
        UUID userId = UUID.randomUUID();
        AuthService authService = mock(AuthService.class);
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        AuthController authController = new AuthController(authService, rateLimiterService, clientIpResolver);

        Jwt jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn(userId.toString());
        given(authService.authorizeAdminPin(userId, "123456")).willReturn("pin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authController.authorizePin(jwt, new UserUpdatePinRequest("123456"), response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("pin_auth_token=pin-token")
                .contains("Max-Age=900")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=None");
    }

    @Test
    void authorizePin_WrongPin_PropagatesExceptionAndSetsNoCookie() {
        UUID userId = UUID.randomUUID();
        AuthService authService = mock(AuthService.class);
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        AuthController authController = new AuthController(authService, rateLimiterService, clientIpResolver);

        Jwt jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn(userId.toString());
        given(authService.authorizeAdminPin(userId, "999999"))
                .willThrow(new InvalidAdminPinException());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authController.authorizePin(jwt, new UserUpdatePinRequest("999999"), response))
                 .isInstanceOf(InvalidAdminPinException.class);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    private UserAuthResponse createAuthResult() {
        UserResponse userResponse = new UserResponse(UUID.randomUUID(), null, "John Doe", "test@test.com",
                "username", Role.ADMIN, true);
        return new UserAuthResponse("fake-jwt-token", new UserLoginInternalResponse(userResponse, null));
    }
}
