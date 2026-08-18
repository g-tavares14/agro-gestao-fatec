package com.agrogestao.finance;

import com.agrogestao.domain.FinancialEntry;
import com.agrogestao.domain.ProductionCost;
import com.agrogestao.domain.enums.FinanceType;
import com.agrogestao.finance.dto.FinanceStatementResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinanceStatementService {

    public FinanceStatementResponse compute(List<FinancialEntry> entries, List<ProductionCost> costs) {
        BigDecimal receitaBruta = BigDecimal.ZERO;
        BigDecimal outrasDespesas = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();

        for (FinancialEntry entry : entries) {
            BigDecimal amount = amountOrZero(entry.getAmount());
            if (entry.getType() == FinanceType.RECEITA) {
                receitaBruta = receitaBruta.add(amount);
            } else if (entry.getType() == FinanceType.DESPESA) {
                outrasDespesas = outrasDespesas.add(amount);
            }
            String key = entry.getType() + ":" + (entry.getCategory() == null || entry.getCategory().isBlank()
                    ? "SEM_CATEGORIA"
                    : entry.getCategory());
            byCategory.merge(key, amount, BigDecimal::add);
        }

        BigDecimal custosProducao = BigDecimal.ZERO;
        for (ProductionCost cost : costs) {
            BigDecimal amount = amountOrZero(cost.getAmount());
            custosProducao = custosProducao.add(amount);
            String key = "CUSTO:" + (cost.getCategory() == null ? "OUTRO" : cost.getCategory().name());
            byCategory.merge(key, amount, BigDecimal::add);
        }

        BigDecimal resultado = receitaBruta.subtract(custosProducao).subtract(outrasDespesas);
        return new FinanceStatementResponse(receitaBruta, custosProducao, outrasDespesas, resultado, byCategory);
    }

    private static BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
