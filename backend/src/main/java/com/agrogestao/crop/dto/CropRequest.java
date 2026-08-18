package com.agrogestao.crop.dto;

import com.agrogestao.domain.enums.CropStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CropRequest(
        UUID propertyId,

        @NotBlank(message = "Campo obrigatório")
        String name,

        String variety,

        String irrigationSystem,

        @DecimalMin(value = "0.0", inclusive = true, message = "Valor deve ser maior ou igual a zero")
        BigDecimal areaHa,

        LocalDate plantingDate,

        LocalDate expectedHarvestDate,

        CropStatus status,

        String expectedYield,

        String notes
) {
}
