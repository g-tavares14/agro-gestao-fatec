package com.agrogestao.auth;

import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.dto.ExchangeRequest;
import com.agrogestao.auth.dto.GoogleEnabledResponse;
import com.agrogestao.auth.dto.LoginRequest;
import com.agrogestao.auth.dto.RegisterRequest;
import com.agrogestao.auth.dto.UserResponse;
import com.agrogestao.config.AppProperties;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AppProperties appProperties;

    public AuthController(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.appProperties = appProperties;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/oauth/exchange")
    public AuthResponse exchangeOAuth(@Valid @RequestBody ExchangeRequest request) {
        return authService.exchangeOAuth(request.code());
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.me();
    }

    @GetMapping("/google-enabled")
    public GoogleEnabledResponse googleEnabled() {
        return new GoogleEnabledResponse(appProperties.getGoogle().isEnabled());
    }
}
