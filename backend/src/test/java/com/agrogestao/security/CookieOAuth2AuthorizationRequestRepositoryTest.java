package com.agrogestao.security;

import com.agrogestao.config.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieOAuth2AuthorizationRequestRepositoryTest {

    private CookieOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-that-is-at-least-32-bytes");
        repository = new CookieOAuth2AuthorizationRequestRepository(new JwtService(properties), new ObjectMapper());
    }

    @Test
    void saveAndLoadRoundTrip() {
        OAuth2AuthorizationRequest original = sampleRequest();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(original, request, response);

        jakarta.servlet.http.Cookie cookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertEquals(180, cookie.getMaxAge());
        request.setCookies(cookie);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);
        assertNotNull(loaded);
        assertEquals(original.getAuthorizationUri(), loaded.getAuthorizationUri());
        assertEquals(original.getClientId(), loaded.getClientId());
        assertEquals(original.getRedirectUri(), loaded.getRedirectUri());
        assertEquals(original.getState(), loaded.getState());
        assertEquals(original.getScopes(), loaded.getScopes());
        assertEquals(original.getAuthorizationRequestUri(), loaded.getAuthorizationRequestUri());
    }

    @Test
    void tamperedSignatureIsTreatedAsMissing() {
        OAuth2AuthorizationRequest original = sampleRequest();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(original, request, response);

        jakarta.servlet.http.Cookie cookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(cookie);
        cookie.setValue(cookie.getValue() + "x");
        request.setCookies(cookie);

        assertNull(repository.loadAuthorizationRequest(request));
    }

    private static OAuth2AuthorizationRequest sampleRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .scopes(Set.of("openid", "email", "profile"))
                .state("state-value")
                .attributes(java.util.Map.of("registration_id", "google"))
                .build();
    }
}
