package com.agrogestao.finance;

import com.agrogestao.domain.FinancialEntry;
import com.agrogestao.domain.ProductionCost;
import com.agrogestao.domain.enums.FinanceType;
import com.agrogestao.domain.enums.ItemCategory;
import com.agrogestao.finance.dto.FinanceStatementResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanceStatementServiceTest {

    private final FinanceStatementService service = new FinanceStatementService();

    @Test
    void computeSubtractsProductionCostsAndExpensesFromRevenues() {
        FinancialEntry receita = entry(FinanceType.RECEITA, "1000.00");
        FinancialEntry despesa = entry(FinanceType.DESPESA, "200.00");
        ProductionCost custo = cost("300.00");

        FinanceStatementResponse statement = service.compute(List.of(receita, despesa), List.of(custo));

        assertEquals(0, new BigDecimal("1000.00").compareTo(statement.receitaBruta()));
        assertEquals(0, new BigDecimal("300.00").compareTo(statement.custosProducao()));
        assertEquals(0, new BigDecimal("200.00").compareTo(statement.outrasDespesas()));
        assertEquals(0, new BigDecimal("500.00").compareTo(statement.resultado()));
    }

    @Test
    void computeTreatsNullAmountsAsZero() {
        FinancialEntry receita = entry(FinanceType.RECEITA, "80.50");
        FinancialEntry despesaSemValor = new FinancialEntry();
        despesaSemValor.setType(FinanceType.DESPESA);
        despesaSemValor.setAmount(null);
        ProductionCost custoSemValor = new ProductionCost();
        custoSemValor.setAmount(null);
        custoSemValor.setCategory(ItemCategory.INSUMO);

        FinanceStatementResponse statement = service.compute(List.of(receita, despesaSemValor), List.of(custoSemValor));

        assertEquals(0, new BigDecimal("80.50").compareTo(statement.resultado()));
    }

    private static FinancialEntry entry(FinanceType type, String amount) {
        FinancialEntry entry = new FinancialEntry();
        entry.setType(type);
        entry.setAmount(new BigDecimal(amount));
        entry.setCategory(type.name());
        return entry;
    }

    private static ProductionCost cost(String amount) {
        ProductionCost cost = new ProductionCost();
        cost.setAmount(new BigDecimal(amount));
        cost.setCategory(ItemCategory.INSUMO);
        return cost;
    }
}
