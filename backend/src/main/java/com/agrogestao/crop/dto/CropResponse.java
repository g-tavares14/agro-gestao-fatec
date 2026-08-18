package com.agrogestao.crop.dto;

import com.agrogestao.domain.Crop;
import com.agrogestao.domain.enums.CropOrigin;
import com.agrogestao.domain.enums.CropStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CropResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        String name,
        String variety,
        String irrigationSystem,
        BigDecimal areaHa,
        LocalDate plantingDate,
        LocalDate expectedHarvestDate,
        CropStatus status,
        String expectedYield,
        String notes,
        CropOrigin origin,
        UUID sourcePdfId,
        List<ItemResponse> acoesMecanicas,
        List<ItemResponse> acoesManuais,
        List<ItemResponse> insumos,
        List<ItemResponse> outros,
        Instant createdAt,
        Instant updatedAt
) {
    public CropResponse {
        acoesMecanicas = acoesMecanicas == null ? List.of() : List.copyOf(acoesMecanicas);
        acoesManuais = acoesManuais == null ? List.of() : List.copyOf(acoesManuais);
        insumos = insumos == null ? List.of() : List.copyOf(insumos);
        outros = outros == null ? List.of() : List.copyOf(outros);
    }

    public static CropResponse from(Crop crop, List<ItemResponse> acoesMecanicas, List<ItemResponse> acoesManuais,
                                    List<ItemResponse> insumos, List<ItemResponse> outros) {
        return new CropResponse(
                crop.getId(),
                crop.getProperty().getId(),
                crop.getProperty().getName(),
                crop.getName(),
                crop.getVariety(),
                crop.getIrrigationSystem(),
                crop.getAreaHa(),
                crop.getPlantingDate(),
                crop.getExpectedHarvestDate(),
                crop.getStatus(),
                crop.getExpectedYield(),
                crop.getNotes(),
                crop.getOrigin(),
                crop.getSourcePdf() == null ? null : crop.getSourcePdf().getId(),
                acoesMecanicas,
                acoesManuais,
                insumos,
                outros,
                crop.getCreatedAt(),
                crop.getUpdatedAt()
        );
    }
}
