package com.agrogestao.property.dto;

import com.agrogestao.domain.Property;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        String name,
        String city,
        String state,
        BigDecimal totalAreaHa,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static PropertyResponse from(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getName(),
                property.getCity(),
                property.getState(),
                property.getTotalAreaHa(),
                property.getDescription(),
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }
}
