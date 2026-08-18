package com.agrogestao.crop.dto;

import java.math.BigDecimal;

public record PdfItemResponse(
        String description,
        String unit,
        BigDecimal quantity,
        BigDecimal unitValue,
        BigDecimal totalValue
) {
}
