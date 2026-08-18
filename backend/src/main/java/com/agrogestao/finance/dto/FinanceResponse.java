package com.agrogestao.finance.dto;

import com.agrogestao.domain.FinancialEntry;
import com.agrogestao.domain.enums.FinanceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FinanceResponse(
        UUID id,
        UUID propertyId,
        UUID cropId,
        FinanceType type,
        String category,
        String description,
        BigDecimal amount,
        LocalDate date,
        Instant createdAt,
        Instant updatedAt
) {
    public static FinanceResponse from(FinancialEntry entry) {
        return new FinanceResponse(
                entry.getId(),
                entry.getProperty().getId(),
                entry.getCrop() == null ? null : entry.getCrop().getId(),
                entry.getType(),
                entry.getCategory(),
                entry.getDescription(),
                entry.getAmount(),
                entry.getDate(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
