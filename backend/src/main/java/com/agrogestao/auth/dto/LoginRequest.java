package com.agrogestao.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Campo obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Campo obrigatório")
        String password
) {
}
