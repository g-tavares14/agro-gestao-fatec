package com.agrogestao.dashboard;

import com.agrogestao.common.AppClock;
import com.agrogestao.dashboard.dto.DashboardResponse;
import com.agrogestao.dashboard.dto.DashboardResponse.DashboardCrop;
import com.agrogestao.dashboard.dto.DashboardResponse.DashboardKpis;
import com.agrogestao.dashboard.dto.DashboardResponse.DashboardProperty;
import com.agrogestao.domain.Crop;
import com.agrogestao.domain.FinancialEntry;
import com.agrogestao.domain.ProductionCost;
import com.agrogestao.domain.Property;
import com.agrogestao.domain.enums.FinanceType;
import com.agrogestao.repository.CropRepository;
import com.agrogestao.repository.FinancialEntryRepository;
import com.agrogestao.repository.ProductionCostRepository;
import com.agrogestao.repository.PropertyRepository;
import com.agrogestao.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final CropRepository cropRepository;
    private final FinancialEntryRepository financialEntryRepository;
    private final ProductionCostRepository productionCostRepository;

    public DashboardService(
            PropertyRepository propertyRepository,
            CropRepository cropRepository,
            FinancialEntryRepository financialEntryRepository,
            ProductionCostRepository productionCostRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.cropRepository = cropRepository;
        this.financialEntryRepository = financialEntryRepository;
        this.productionCostRepository = productionCostRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<Property> properties = propertyRepository.findByUserIdOrderByNameAsc(userId);
        List<Crop> crops = cropRepository.findByPropertyUserIdOrderByNameAsc(userId);

        Map<UUID, Long> activeByProperty = crops.stream()
                .filter(crop -> crop.getStatus() != null && crop.getStatus().isActive())
                .collect(Collectors.groupingBy(crop -> crop.getProperty().getId(), Collectors.counting()));

        List<DashboardProperty> propertyCards = properties.stream()
                .map(property -> new DashboardProperty(
                        property.getId(),
                        property.getName(),
                        property.getCity(),
                        property.getState(),
                        property.getTotalAreaHa(),
                        activeByProperty.getOrDefault(property.getId(), 0L).intValue()
                ))
                .toList();

        List<DashboardCrop> activeCrops = new ArrayList<>();
        BigDecimal areaCultivada = BigDecimal.ZERO;
        for (Crop crop : crops) {
            if (crop.getStatus() == null || !crop.getStatus().isActive()) {
                continue;
            }
            activeCrops.add(new DashboardCrop(
                    crop.getId(),
                    crop.getName(),
                    crop.getStatus().name(),
                    crop.getAreaHa(),
                    crop.getProperty().getName()
            ));
            if (crop.getAreaHa() != null) {
                areaCultivada = areaCultivada.add(crop.getAreaHa());
            }
        }

        YearMonth month = AppClock.currentMonth();
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        BigDecimal receitaMes = BigDecimal.ZERO;
        BigDecimal despesaMes = BigDecimal.ZERO;
        for (FinancialEntry entry : financialEntryRepository.findByPropertyUserIdAndDateBetween(userId, start, end)) {
            BigDecimal amount = entry.getAmount() == null ? BigDecimal.ZERO : entry.getAmount();
            if (entry.getType() == FinanceType.RECEITA) {
                receitaMes = receitaMes.add(amount);
            } else if (entry.getType() == FinanceType.DESPESA) {
                despesaMes = despesaMes.add(amount);
            }
        }
        for (ProductionCost cost : productionCostRepository.findByCropPropertyUserIdAndDateBetween(userId, start, end)) {
            despesaMes = despesaMes.add(cost.getAmount() == null ? BigDecimal.ZERO : cost.getAmount());
        }

        DashboardKpis kpis = new DashboardKpis(
                receitaMes,
                despesaMes,
                receitaMes.subtract(despesaMes),
                activeCrops.size(),
                areaCultivada
        );
        return new DashboardResponse(propertyCards, activeCrops, kpis);
    }
}
