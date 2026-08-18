package com.agrogestao.crop.dto;

import java.util.List;
import java.util.UUID;

public record PdfAnalyzeResponse(
        UUID analysisId,
        PdfExtractedResponse extracted,
        List<String> warnings
) {
    public PdfAnalyzeResponse {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
