package com.agrogestao.cost.dto;

import com.agrogestao.domain.enums.ItemCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CostRequest(
        @NotBlank(message = "Campo obrigatório")
        String description,

        @NotNull(message = "Campo obrigatório")
        ItemCategory category,

        @NotNull(message = "Campo obrigatório")
        @DecimalMin(value = "0.0", inclusive = true, message = "Valor deve ser maior ou igual a zero")
        BigDecimal amount,

        @NotNull(message = "Campo obrigatório")
        LocalDate date
) {
}
