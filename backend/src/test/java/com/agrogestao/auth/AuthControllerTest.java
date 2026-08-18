package com.agrogestao.auth;

import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.dto.UserResponse;
import com.agrogestao.config.AppProperties;
import com.agrogestao.domain.enums.AuthProvider;
import com.agrogestao.exception.ApiExceptionHandler;
import com.agrogestao.exception.ConflictException;
import com.agrogestao.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties(AppProperties.class)
@Import(ApiExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerDuplicateEmailReturns409() throws Exception {
        when(authService.register(any())).thenThrow(new ConflictException("E-mail já cadastrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ana","email":"ana@example.com","password":"password1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }

    @Test
    void loginInvalidCredentialsReturns401() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("Credenciais inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"wrongpass"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    void googleEnabledReturnsFalseByDefault() throws Exception {
        mockMvc.perform(get("/api/auth/google-enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void registerValidationFailureReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ana","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void exchangeInvalidCodeReturns401() throws Exception {
        when(authService.exchangeOAuth(any())).thenThrow(new UnauthorizedException("Código OAuth inválido ou expirado"));

        mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"used-or-expired"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Código OAuth inválido ou expirado"));
    }

    @Test
    void registerSuccessReturns201() throws Exception {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        AuthResponse response = new AuthResponse(
                "token-value",
                new UserResponse(userId, "Ana", "ana@example.com", AuthProvider.LOCAL)
        );
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ana","email":"ana@example.com","password":"password1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-value"))
                .andExpect(jsonPath("$.user.email").value("ana@example.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }
}
