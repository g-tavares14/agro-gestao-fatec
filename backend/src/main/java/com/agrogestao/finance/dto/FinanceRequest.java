package com.agrogestao.finance.dto;

import com.agrogestao.domain.enums.FinanceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinanceRequest(
        @NotNull(message = "Campo obrigatório")
        UUID propertyId,

        UUID cropId,

        @NotNull(message = "Campo obrigatório")
        FinanceType type,

        String category,

        @NotBlank(message = "Campo obrigatório")
        String description,

        @NotNull(message = "Campo obrigatório")
        @DecimalMin(value = "0.0", inclusive = true, message = "Valor deve ser maior ou igual a zero")
        BigDecimal amount,

        @NotNull(message = "Campo obrigatório")
        LocalDate date
) {
}
