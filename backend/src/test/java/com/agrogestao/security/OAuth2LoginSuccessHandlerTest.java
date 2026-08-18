package com.agrogestao.security;

import com.agrogestao.auth.AuthService;
import com.agrogestao.config.AppProperties;
import com.agrogestao.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private AuthService authService;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl("http://localhost");
        handler = new OAuth2LoginSuccessHandler(authService, appProperties);
    }

    @Test
    void successRedirectsToSpaCallbackWithOneTimeCode() throws Exception {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        User user = new User();
        user.setId(userId);
        user.setEmail("ana@example.com");
        when(authService.upsertGoogleUser("google-sub-1", "ana@example.com", "Ana")).thenReturn(user);
        when(authService.issueOAuthCode(userId)).thenReturn("one-time-code");

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                authentication(oauthUser(true, "ana@example.com", "Ana", "google-sub-1"))
        );

        String redirected = response.getRedirectedUrl();
        assertEquals("http://localhost/auth/callback?code=one-time-code", redirected);
        assertTrue(redirected.contains("code="));
        assertFalse(redirected.contains("token="));
        assertFalse(redirected.contains("access_token="));
    }

    @Test
    void unverifiedEmailRedirectsToCallbackError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                authentication(oauthUser(false, "ana@example.com", "Ana", "google-sub-1"))
        );

        assertEquals("http://localhost/auth/callback?error=email_not_verified", response.getRedirectedUrl());
        verifyNoInteractions(authService);
    }

    @Test
    void upsertFailureRedirectsToOauthFailed() throws Exception {
        when(authService.upsertGoogleUser("google-sub-1", "ana@example.com", "Ana"))
                .thenThrow(new IllegalStateException("db"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                authentication(oauthUser(true, "ana@example.com", "Ana", "google-sub-1"))
        );

        assertEquals("http://localhost/auth/callback?error=oauth_failed", response.getRedirectedUrl());
        assertTrue(response.getRedirectedUrl().contains("/auth/callback"));
    }

    private static Authentication authentication(OAuth2User principal) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        return authentication;
    }

    private static OAuth2User oauthUser(boolean emailVerified, String email, String name, String sub) {
        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email_verified")).thenReturn(emailVerified);
        if (emailVerified) {
            when(oauthUser.getAttribute("email")).thenReturn(email);
            when(oauthUser.getAttribute("name")).thenReturn(name);
            when(oauthUser.getAttribute("given_name")).thenReturn(null);
            when(oauthUser.getAttribute("sub")).thenReturn(sub);
        }
        return oauthUser;
    }
}
