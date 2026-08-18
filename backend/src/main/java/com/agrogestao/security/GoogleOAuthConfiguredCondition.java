package com.agrogestao.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class GoogleOAuthConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String clientId = firstNonBlank(
                env.getProperty("app.google.client-id"),
                env.getProperty("GOOGLE_CLIENT_ID")
        );
        String clientSecret = firstNonBlank(
                env.getProperty("app.google.client-secret"),
                env.getProperty("GOOGLE_CLIENT_SECRET")
        );
        return clientId != null && clientSecret != null;
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
}
