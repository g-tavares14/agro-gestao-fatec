package com.agrogestao.property.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PropertyRequest(
        @NotBlank(message = "Campo obrigatório")
        String name,

        String city,

        @Size(min = 2, max = 2, message = "UF deve ter 2 letras")
        @Pattern(regexp = "[A-Za-z]{2}", message = "UF deve ter 2 letras")
        String state,

        @DecimalMin(value = "0.0", inclusive = true, message = "Valor deve ser maior ou igual a zero")
        BigDecimal totalAreaHa,

        String description
) {
}
