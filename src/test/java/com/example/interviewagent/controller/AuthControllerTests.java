package com.example.interviewagent.controller;

import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.redis.LoginAttemptService;
import com.example.interviewagent.security.RefreshTokenCookieFactory;
import com.example.interviewagent.service.AuthService;
import com.example.interviewagent.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTests {

    @Mock
    private AuthService authService;
    @Mock
    private TokenService tokenService;
    @Mock
    private RefreshTokenCookieFactory cookieFactory;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpServletResponse response;

    @Test
    void failedCredentialsAreRecordedForUsernameAndClientIp() {
        AuthController controller = new AuthController(
                authService, tokenService, cookieFactory, loginAttemptService);
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("alice");
        request.setPassword("wrong-password");
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authService.login("alice", "wrong-password"))
                .thenThrow(new BusinessException(401, "用户名或密码错误"));

        assertThrows(BusinessException.class,
                () -> controller.login(request, "browser", servletRequest, response));

        verify(loginAttemptService).ensureAllowed("alice", "127.0.0.1");
        verify(loginAttemptService).recordFailure("alice", "127.0.0.1");
    }
}
