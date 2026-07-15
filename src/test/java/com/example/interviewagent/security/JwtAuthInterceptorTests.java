package com.example.interviewagent.security;

import com.example.interviewagent.config.JwtProperties;
import com.example.interviewagent.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthInterceptorTests {

    private JwtTokenProvider tokenProvider;
    private AccessTokenSupport accessTokenSupport;
    private JwtAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-key-with-more-than-32-bytes");
        tokenProvider = new JwtTokenProvider(properties);
        accessTokenSupport = new AccessTokenSupport(tokenProvider);
        interceptor = new JwtAuthInterceptor(accessTokenSupport, tokenProvider);
    }

    @Test
    void validAccessTokenInjectsCurrentUserAndRole() {
        String token = tokenProvider.createAccessToken(42L, "USER");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(allowed);
        assertEquals(42L, request.getAttribute("authUserId"));
        assertEquals("USER", request.getAttribute("authRole"));
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refreshToken = tokenProvider.createRefreshToken(42L, "browser", "jti-1").token();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> accessTokenSupport.authenticate(request));

        assertEquals(401, exception.getCode());
    }
}
