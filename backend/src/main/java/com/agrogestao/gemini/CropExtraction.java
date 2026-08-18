package com.agrogestao.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CropExtraction(
        String nomeCultura,
        String variedade,
        String sistemaIrrigacao,
        BigDecimal areaHectares,
        LocalDate dataPlantio,
        String produtividadeEsperada,
        BigDecimal custoTotal,
        BigDecimal custoUnidadeComercializacao,
        String observacoes,
        List<String> warnings,
        List<CropExtractionItem> acoesMecanicas,
        List<CropExtractionItem> acoesManuais,
        List<CropExtractionItem> insumos,
        List<CropExtractionItem> outros
) {
    public CropExtraction {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        acoesMecanicas = acoesMecanicas == null ? List.of() : List.copyOf(acoesMecanicas);
        acoesManuais = acoesManuais == null ? List.of() : List.copyOf(acoesManuais);
        insumos = insumos == null ? List.of() : List.copyOf(insumos);
        outros = outros == null ? List.of() : List.copyOf(outros);
    }
}
