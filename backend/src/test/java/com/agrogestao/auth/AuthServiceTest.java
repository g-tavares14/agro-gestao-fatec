package com.agrogestao.auth;

import com.agrogestao.auth.dto.RegisterRequest;
import com.agrogestao.domain.User;
import com.agrogestao.domain.enums.AuthProvider;
import com.agrogestao.exception.ConflictException;
import com.agrogestao.exception.UnauthorizedException;
import com.agrogestao.repository.UserRepository;
import com.agrogestao.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private OAuthCodeService oauthCodeService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, oauthCodeService);
    }

    @Test
    void registerDuplicateEmailThrowsConflict() {
        RegisterRequest request = new RegisterRequest("Ana", "ana@example.com", "password1");
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> authService.register(request));

        assertEquals("E-mail já cadastrado", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(UUID.class), any());
    }

    @Test
    void registerNormalizesEmailBeforeDuplicateCheck() {
        RegisterRequest request = new RegisterRequest("Ana", "  Ana@Example.com ", "password1");
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).existsByEmail(emailCaptor.capture());
        assertEquals("ana@example.com", emailCaptor.getValue());
    }

    @Test
    void upsertGoogleUserRejectsEmailLinkedToAnotherGoogleSub() {
        User existing = new User();
        existing.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        existing.setEmail("ana@example.com");
        existing.setName("Ana");
        existing.setGoogleSub("google-sub-old");
        existing.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.findByGoogleSub("google-sub-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(existing));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.upsertGoogleUser("google-sub-new", "ana@example.com", "Ana")
        );

        assertEquals("Esta conta já está vinculada a outro login Google", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(UUID.class), any());
        verify(oauthCodeService, never()).issue(any());
    }

    @Test
    void exchangeOAuthRejectsInvalidCode() {
        when(oauthCodeService.consume("used")).thenThrow(new UnauthorizedException(OAuthCodeService.INVALID_CODE));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.exchangeOAuth("used")
        );

        assertEquals(OAuthCodeService.INVALID_CODE, exception.getMessage());
        verify(jwtService, never()).generateToken(any(UUID.class), any());
    }
}
