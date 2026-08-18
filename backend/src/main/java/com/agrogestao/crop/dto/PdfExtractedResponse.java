package com.agrogestao.crop.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PdfExtractedResponse(
        String name,
        String variety,
        String irrigationSystem,
        BigDecimal areaHa,
        LocalDate plantingDate,
        String expectedYield,
        String notes,
        List<PdfItemResponse> acoesMecanicas,
        List<PdfItemResponse> acoesManuais,
        List<PdfItemResponse> insumos,
        List<PdfItemResponse> outros
) {
    public PdfExtractedResponse {
        acoesMecanicas = acoesMecanicas == null ? List.of() : List.copyOf(acoesMecanicas);
        acoesManuais = acoesManuais == null ? List.of() : List.copyOf(acoesManuais);
        insumos = insumos == null ? List.of() : List.copyOf(insumos);
        outros = outros == null ? List.of() : List.copyOf(outros);
    }
}
