package com.agrogestao.auth;

import com.agrogestao.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthCodeService {

    static final String INVALID_CODE = "Código OAuth inválido ou expirado";
    private static final Duration TTL = Duration.ofSeconds(90);

    private record IssuedCode(UUID userId, Instant expiresAt) {
    }

    private final ConcurrentHashMap<String, IssuedCode> codes = new ConcurrentHashMap<>();

    public String issue(UUID userId) {
        evictExpired();
        String code = UUID.randomUUID().toString();
        codes.put(code, new IssuedCode(userId, Instant.now().plus(TTL)));
        return code;
    }

    public UUID consume(String code) {
        if (code == null || code.isBlank()) {
            throw new UnauthorizedException(INVALID_CODE);
        }
        IssuedCode issued = codes.remove(code.trim());
        if (issued == null || Instant.now().isAfter(issued.expiresAt())) {
            throw new UnauthorizedException(INVALID_CODE);
        }
        return issued.userId();
    }

    private void evictExpired() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }
}
