package com.agrogestao.finance.dto;

import java.math.BigDecimal;
import java.util.Map;

public record FinanceStatementResponse(
        BigDecimal receitaBruta,
        BigDecimal custosProducao,
        BigDecimal outrasDespesas,
        BigDecimal resultado,
        Map<String, BigDecimal> byCategory
) {
    public FinanceStatementResponse {
        byCategory = byCategory == null ? Map.of() : Map.copyOf(byCategory);
    }
}
