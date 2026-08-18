package com.agrogestao.gemini;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropExtractionMapperTest {

    private final CropExtractionMapper mapper = new CropExtractionMapper();

    @Test
    void parseFixtureMapsGroupsAndDoesNotInventMissingFields() throws Exception {
        String json = new String(
                Objects.requireNonNull(getClass().getResourceAsStream("/fixtures/alface-extraction.json")).readAllBytes(),
                StandardCharsets.UTF_8
        );

        CropExtraction extraction = mapper.parse(json);

        assertEquals("Alface", extraction.nomeCultura());
        assertNull(extraction.variedade());
        assertNull(extraction.areaHectares());
        assertNull(extraction.dataPlantio());
        assertEquals("4100.00 cx/5kg", extraction.produtividadeEsperada());
        assertEquals(0, new BigDecimal("44076.26").compareTo(extraction.custoTotal()));
        assertTrue(extraction.warnings().stream().anyMatch(warning -> warning.toLowerCase().contains("muda")));
        assertTrue(extraction.warnings().stream().anyMatch(warning -> warning.toLowerCase().contains("energia")));

        assertFalse(extraction.acoesMecanicas().isEmpty());
        assertFalse(extraction.acoesManuais().isEmpty());
        assertFalse(extraction.insumos().isEmpty());
        assertFalse(extraction.outros().isEmpty());
        assertEquals("Preparo do solo (Aração)", extraction.acoesMecanicas().getFirst().descricao());
        assertEquals("Capina (Manual)", extraction.acoesManuais().stream()
                .filter(item -> item.descricao() != null && item.descricao().toLowerCase().contains("capina"))
                .findFirst()
                .orElseThrow()
                .descricao());
        assertTrue(extraction.insumos().stream()
                .anyMatch(item -> item.descricao() != null && item.descricao().toLowerCase().contains("uréia")));
        assertTrue(extraction.insumos().stream()
                .anyMatch(item -> item.descricao() != null && item.descricao().toLowerCase().contains("mudas")));
        assertTrue(extraction.outros().stream()
                .anyMatch(item -> item.descricao() != null && item.descricao().toLowerCase().contains("energia")));
    }
}
