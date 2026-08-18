package com.agrogestao.gemini;

import com.agrogestao.crop.dto.PdfAnalyzeResponse;
import com.agrogestao.crop.dto.PdfExtractedResponse;
import com.agrogestao.crop.dto.PdfItemResponse;
import com.agrogestao.exception.BadRequestException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CropExtractionMapper {

    private final ObjectMapper objectMapper;

    public CropExtractionMapper() {
        this(createDefaultMapper());
    }

    private static ObjectMapper createDefaultMapper() {
        JsonMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        mapper.coercionConfigFor(LogicalType.DateTime)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        mapper.coercionConfigFor(LogicalType.Float)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        mapper.coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        return mapper;
    }

    public CropExtractionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CropExtraction parse(String json) {
        if (json == null || json.isBlank()) {
            throw new BadRequestException("JSON extraído vazio");
        }
        try {
            return objectMapper.readValue(unwrapJson(json), CropExtraction.class);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Não foi possível interpretar o JSON extraído");
        }
    }

    public PdfExtractedResponse toExtracted(CropExtraction extraction) {
        return new PdfExtractedResponse(
                extraction.nomeCultura(),
                extraction.variedade(),
                extraction.sistemaIrrigacao(),
                extraction.areaHectares(),
                extraction.dataPlantio(),
                extraction.produtividadeEsperada(),
                extraction.observacoes(),
                toItems(extraction.acoesMecanicas()),
                toItems(extraction.acoesManuais()),
                toItems(extraction.insumos()),
                toItems(extraction.outros())
        );
    }

    public PdfAnalyzeResponse toResponse(UUID analysisId, CropExtraction extraction) {
        return new PdfAnalyzeResponse(analysisId, toExtracted(extraction), extraction.warnings());
    }

    private static List<PdfItemResponse> toItems(List<CropExtractionItem> items) {
        return items.stream()
                .map(item -> new PdfItemResponse(
                        item.descricao(),
                        item.unidade(),
                        item.quantidade(),
                        item.valorUnitario(),
                        item.valorTotal()
                ))
                .toList();
    }

    private static String unwrapJson(String json) {
        String text = json.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }
}
