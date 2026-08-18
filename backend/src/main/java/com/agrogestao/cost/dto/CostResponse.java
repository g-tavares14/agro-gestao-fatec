package com.agrogestao.cost.dto;

import com.agrogestao.domain.ProductionCost;
import com.agrogestao.domain.enums.CropOrigin;
import com.agrogestao.domain.enums.ItemCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CostResponse(
        UUID id,
        UUID cropId,
        String description,
        ItemCategory category,
        BigDecimal amount,
        LocalDate date,
        CropOrigin origin
) {
    public static CostResponse from(ProductionCost cost) {
        return new CostResponse(
                cost.getId(),
                cost.getCrop().getId(),
                cost.getDescription(),
                cost.getCategory(),
                cost.getAmount(),
                cost.getDate(),
                cost.getOrigin()
        );
    }
}
