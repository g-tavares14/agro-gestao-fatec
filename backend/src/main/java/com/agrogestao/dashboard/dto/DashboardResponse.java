package com.agrogestao.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        List<DashboardProperty> properties,
        List<DashboardCrop> activeCrops,
        DashboardKpis kpis
) {
    public record DashboardProperty(
            UUID id,
            String name,
            String city,
            String state,
            BigDecimal totalAreaHa,
            int activeCrops
    ) {
    }

    public record DashboardCrop(
            UUID id,
            String name,
            String status,
            BigDecimal areaHa,
            String propertyName
    ) {
    }

    public record DashboardKpis(
            BigDecimal receitaMes,
            BigDecimal despesaMes,
            BigDecimal resultadoMes,
            int culturasAtivas,
            BigDecimal areaCultivadaHa
    ) {
    }
}
