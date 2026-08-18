package com.agrogestao.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_MAX_AGE_SECONDS = 180;

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public CookieOAuth2AuthorizationRequestRepository(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request).orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            clearCookie(request, response);
            return;
        }
        Cookie cookie = new Cookie(COOKIE_NAME, serialize(authorizationRequest));
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setSecure(request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        clearCookie(request, response);
        return authorizationRequest;
    }

    private Optional<OAuth2AuthorizationRequest> readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return Optional.ofNullable(deserialize(cookie.getValue()));
            }
        }
        return Optional.empty();
    }

    private static void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(AuthorizationRequestPayload.from(authorizationRequest));
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json);
            return payload + "." + jwtService.hmac(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OAuth2 authorization request", ex);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            int separator = value.indexOf('.');
            if (separator <= 0 || separator == value.length() - 1) {
                return null;
            }
            byte[] json = Base64.getUrlDecoder().decode(value.substring(0, separator));
            String signature = value.substring(separator + 1);
            if (!jwtService.hmacMatches(json, signature)) {
                return null;
            }
            AuthorizationRequestPayload payload = objectMapper.readValue(json, AuthorizationRequestPayload.class);
            return payload.toAuthorizationRequest();
        } catch (Exception ex) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AuthorizationRequestPayload(
            String authorizationUri,
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String state,
            String grantType,
            Map<String, Object> additionalParameters,
            Map<String, Object> attributes,
            String authorizationRequestUri
    ) {
        static AuthorizationRequestPayload from(OAuth2AuthorizationRequest request) {
            return new AuthorizationRequestPayload(
                    request.getAuthorizationUri(),
                    request.getClientId(),
                    request.getRedirectUri(),
                    request.getScopes(),
                    request.getState(),
                    request.getGrantType() == null ? null : request.getGrantType().getValue(),
                    request.getAdditionalParameters(),
                    request.getAttributes(),
                    request.getAuthorizationRequestUri()
            );
        }

        OAuth2AuthorizationRequest toAuthorizationRequest() {
            if (!StringUtils.hasText(authorizationUri) || !StringUtils.hasText(clientId)) {
                return null;
            }
            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes == null ? Set.of() : scopes)
                    .state(state)
                    .additionalParameters(additionalParameters == null ? Map.of() : additionalParameters)
                    .attributes(attributes == null ? Map.of() : attributes);
            if (StringUtils.hasText(authorizationRequestUri)) {
                builder.authorizationRequestUri(authorizationRequestUri);
            }
            return builder.build();
        }
    }
}
