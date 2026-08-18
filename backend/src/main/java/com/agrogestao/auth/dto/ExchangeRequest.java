package com.agrogestao.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeRequest(
        @NotBlank(message = "Campo obrigatório")
        String code
) {
}
