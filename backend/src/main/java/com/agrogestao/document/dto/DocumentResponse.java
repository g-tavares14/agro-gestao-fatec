package com.agrogestao.document.dto;

import com.agrogestao.domain.Document;
import com.agrogestao.domain.enums.DocumentKind;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID propertyId,
        UUID cropId,
        String originalName,
        String contentType,
        long sizeBytes,
        DocumentKind kind,
        Instant createdAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getProperty().getId(),
                document.getCrop() == null ? null : document.getCrop().getId(),
                document.getOriginalName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getKind(),
                document.getCreatedAt()
        );
    }
}
