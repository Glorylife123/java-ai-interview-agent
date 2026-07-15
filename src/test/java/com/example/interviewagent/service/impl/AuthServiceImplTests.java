package com.example.interviewagent.service.impl;

import com.example.interviewagent.entity.User;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.UserMapper;
import com.example.interviewagent.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTests {

    @Mock
    private UserMapper userMapper;
    @Mock
    private TokenService tokenService;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(userMapper, passwordEncoder, tokenService);
    }

    @Test
    void publicRegistrationAlwaysCreatesUserRole() {
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        User stored = new User();
        stored.setId(10L);
        stored.setUsername("alice");
        stored.setRole("USER");
        when(userMapper.selectById(10L)).thenReturn(stored);

        User result = authService.register("alice", "secret123", "Alice");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User inserted = captor.getValue();
        assertEquals("USER", inserted.getRole());
        assertNotEquals("secret123", inserted.getPasswordHash());
        assertTrue(passwordEncoder.matches("secret123", inserted.getPasswordHash()));
        assertEquals("USER", result.getRole());
    }

    @Test
    void duplicateUsernameIsRejectedBeforeInsert() {
        when(userMapper.selectByUsername("alice")).thenReturn(new User());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register("alice", "secret123", null));

        assertEquals(400, exception.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginValidatesBcryptPasswordAndHidesHash() {
        User stored = new User();
        stored.setId(10L);
        stored.setUsername("alice");
        stored.setPasswordHash(passwordEncoder.encode("secret123"));
        stored.setRole("USER");
        stored.setStatus(1);
        when(userMapper.selectByUsername("alice")).thenReturn(stored);

        User result = authService.login("alice", "secret123");

        assertEquals(10L, result.getId());
        assertNull(result.getPasswordHash());
    }
}
