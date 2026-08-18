package com.agrogestao.config;

import com.agrogestao.gemini.GeminiCropExtractor;
import com.agrogestao.gemini.GoogleGenAiCropExtractor;
import com.agrogestao.gemini.UnsupportedGeminiCropExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Bean
    public GeminiCropExtractor geminiCropExtractor(GeminiProperties properties) {
        if (properties.isConfigured()) {
            return new GoogleGenAiCropExtractor(properties);
        }
        return new UnsupportedGeminiCropExtractor();
    }
}
