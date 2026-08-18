package com.agrogestao.crop.dto;

import com.agrogestao.domain.PlannedItem;
import com.agrogestao.domain.enums.ItemCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        String description,
        String unit,
        BigDecimal quantity,
        BigDecimal unitValue,
        BigDecimal totalValue,
        ItemCategory category
) {
    public static ItemResponse from(PlannedItem item) {
        return new ItemResponse(
                item.getId(),
                item.getDescription(),
                item.getUnit(),
                item.getQuantity(),
                item.getUnitValue(),
                item.getTotalValue(),
                item.getCategory()
        );
    }
}
