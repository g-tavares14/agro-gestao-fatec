package com.agrogestao.gemini;

import com.agrogestao.exception.ServiceUnavailableException;

public class UnsupportedGeminiCropExtractor implements GeminiCropExtractor {

    public static final String UNAVAILABLE_MESSAGE = "Análise de PDF indisponível: configure GEMINI_API_KEY";

    @Override
    public String extractJson(byte[] pdfContent) {
        throw new ServiceUnavailableException(UNAVAILABLE_MESSAGE);
    }
}
