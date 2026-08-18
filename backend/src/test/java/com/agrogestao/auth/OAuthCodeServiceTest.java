package com.agrogestao.auth;

import com.agrogestao.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuthCodeServiceTest {

    private final OAuthCodeService oauthCodeService = new OAuthCodeService();

    @Test
    void consumeReturnsUserIdOnce() {
        UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        String code = oauthCodeService.issue(userId);

        assertEquals(userId, oauthCodeService.consume(code));
        UnauthorizedException reused = assertThrows(UnauthorizedException.class, () -> oauthCodeService.consume(code));
        assertEquals(OAuthCodeService.INVALID_CODE, reused.getMessage());
    }

    @Test
    void unknownCodeIsUnauthorized() {
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> oauthCodeService.consume("missing")
        );
        assertEquals(OAuthCodeService.INVALID_CODE, exception.getMessage());
    }
}
