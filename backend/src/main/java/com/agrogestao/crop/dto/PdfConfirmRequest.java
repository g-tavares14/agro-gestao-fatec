package com.agrogestao.crop.dto;

import com.agrogestao.domain.enums.CropStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PdfConfirmRequest(
        @NotNull(message = "Campo obrigatório")
        UUID analysisId,

        @NotNull(message = "Campo obrigatório")
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

        String notes,

        @Valid
        List<ItemRequest> acoesMecanicas,

        @Valid
        List<ItemRequest> acoesManuais,

        @Valid
        List<ItemRequest> insumos,

        @Valid
        List<ItemRequest> outros,

        Boolean importCosts
) {
    public PdfConfirmRequest {
        acoesMecanicas = acoesMecanicas == null ? List.of() : List.copyOf(acoesMecanicas);
        acoesManuais = acoesManuais == null ? List.of() : List.copyOf(acoesManuais);
        insumos = insumos == null ? List.of() : List.copyOf(insumos);
        outros = outros == null ? List.of() : List.copyOf(outros);
        importCosts = importCosts != null && importCosts;
    }
}
