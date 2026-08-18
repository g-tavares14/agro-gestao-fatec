package com.agrogestao.crop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ItemRequest(
        @NotBlank(message = "Campo obrigatório")
        String description,

        String unit,

        @DecimalMin(value = "0.0", inclusive = true, message = "Valor deve ser maior ou igual a zero")
        BigDecimal quantity,

        @DecimalMin(value = "0.0", inclusive = true, message = "Valor deve ser maior ou igual a zero")
        BigDecimal unitValue,

        @DecimalMin(value = "0.0", inclusive = true, message = "Valor deve ser maior ou igual a zero")
        BigDecimal totalValue
) {
}
