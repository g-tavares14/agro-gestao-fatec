package com.agrogestao.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Campo obrigatório")
        String name,

        @NotBlank(message = "Campo obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Campo obrigatório")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password
) {
}
