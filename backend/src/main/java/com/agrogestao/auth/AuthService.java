package com.agrogestao.auth;

import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.dto.LoginRequest;
import com.agrogestao.auth.dto.RegisterRequest;
import com.agrogestao.auth.dto.UserResponse;
import com.agrogestao.domain.User;
import com.agrogestao.domain.enums.AuthProvider;
import com.agrogestao.exception.ConflictException;
import com.agrogestao.exception.UnauthorizedException;
import com.agrogestao.repository.UserRepository;
import com.agrogestao.security.JwtService;
import com.agrogestao.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    static final String GENERIC_LOGIN_FAILURE = "Credenciais inválidas";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OAuthCodeService oauthCodeService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OAuthCodeService oauthCodeService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.oauthCodeService = oauthCodeService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user = userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(GENERIC_LOGIN_FAILURE));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(GENERIC_LOGIN_FAILURE);
        }
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Autenticação necessária"));
        return UserResponse.from(user);
    }

    public String issueOAuthCode(UUID userId) {
        return oauthCodeService.issue(userId);
    }

    @Transactional(readOnly = true)
    public AuthResponse exchangeOAuth(String code) {
        UUID userId = oauthCodeService.consume(code);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(OAuthCodeService.INVALID_CODE));
        return toAuthResponse(user);
    }

    @Transactional
    public User upsertGoogleUser(String googleSub, String email, String name) {
        if (googleSub == null || googleSub.isBlank() || email == null || email.isBlank()) {
            throw new UnauthorizedException("Não foi possível autenticar com o Google");
        }
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByGoogleSub(googleSub)
                .or(() -> userRepository.findByEmail(normalizedEmail))
                .map(existing -> linkGoogleAccount(existing, googleSub, name))
                .orElseGet(() -> createGoogleUser(googleSub, normalizedEmail, name));
    }

    private User linkGoogleAccount(User existing, String googleSub, String name) {
        if (existing.getGoogleSub() != null && !existing.getGoogleSub().equals(googleSub)) {
            throw new UnauthorizedException("Esta conta já está vinculada a outro login Google");
        }
        boolean changed = false;
        if (existing.getGoogleSub() == null) {
            existing.setGoogleSub(googleSub);
            changed = true;
        }
        if (existing.getAuthProvider() == AuthProvider.LOCAL) {
            existing.setAuthProvider(AuthProvider.BOTH);
            changed = true;
        }
        if ((existing.getName() == null || existing.getName().isBlank()) && name != null && !name.isBlank()) {
            existing.setName(name.trim());
            changed = true;
        }
        return changed ? userRepository.save(existing) : existing;
    }

    private User createGoogleUser(String googleSub, String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName(name == null || name.isBlank() ? email : name.trim());
        user.setGoogleSub(googleSub);
        user.setAuthProvider(AuthProvider.GOOGLE);
        return userRepository.save(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
