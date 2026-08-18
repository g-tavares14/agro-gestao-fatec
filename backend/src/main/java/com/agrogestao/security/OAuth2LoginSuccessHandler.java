package com.agrogestao.security;

import com.agrogestao.auth.AuthService;
import com.agrogestao.config.AppProperties;
import com.agrogestao.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final AppProperties appProperties;

    public OAuth2LoginSuccessHandler(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.appProperties = appProperties;
        setDefaultTargetUrl("/app");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        try {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            if (!isEmailVerified(oauthUser)) {
                redirectError(request, response, "email_not_verified");
                return;
            }
            String email = attributeAsString(oauthUser, "email");
            String name = firstNonBlank(
                    attributeAsString(oauthUser, "name"),
                    attributeAsString(oauthUser, "given_name")
            );
            String googleSub = extractSub(oauthUser);

            User user = authService.upsertGoogleUser(googleSub, email, name);
            String code = authService.issueOAuthCode(user.getId());
            String target = trimTrailingSlash(appProperties.getFrontendUrl())
                    + "/auth/callback?code="
                    + URLEncoder.encode(code, StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response, target);
        } catch (RuntimeException ex) {
            redirectError(request, response, "oauth_failed");
        }
    }

    private void redirectError(HttpServletRequest request, HttpServletResponse response, String code)
            throws IOException {
        getRedirectStrategy().sendRedirect(
                request,
                response,
                trimTrailingSlash(appProperties.getFrontendUrl()) + "/auth/callback?error=" + code
        );
    }

    private static String attributeAsString(OAuth2User oauthUser, String name) {
        Object value = oauthUser.getAttribute(name);
        return value == null ? null : value.toString();
    }

    private static boolean isEmailVerified(OAuth2User oauthUser) {
        Object verified = oauthUser.getAttribute("email_verified");
        if (verified instanceof Boolean flag) {
            return flag;
        }
        return verified != null && "true".equalsIgnoreCase(verified.toString());
    }

    private static String extractSub(OAuth2User oauthUser) {
        if (oauthUser instanceof OidcUser oidcUser && oidcUser.getSubject() != null) {
            return oidcUser.getSubject();
        }
        Object sub = oauthUser.getAttribute("sub");
        return sub == null ? null : sub.toString();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
