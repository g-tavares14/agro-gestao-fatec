package com.agrogestao.finance;

import com.agrogestao.common.ResourceScope;
import com.agrogestao.common.Strings;
import com.agrogestao.crop.CropService;
import com.agrogestao.domain.Crop;
import com.agrogestao.domain.FinancialEntry;
import com.agrogestao.domain.ProductionCost;
import com.agrogestao.domain.Property;
import com.agrogestao.exception.BadRequestException;
import com.agrogestao.exception.NotFoundException;
import com.agrogestao.finance.dto.FinanceRequest;
import com.agrogestao.finance.dto.FinanceResponse;
import com.agrogestao.finance.dto.FinanceStatementResponse;
import com.agrogestao.property.PropertyService;
import com.agrogestao.repository.FinancialEntryRepository;
import com.agrogestao.repository.ProductionCostRepository;
import com.agrogestao.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FinanceService {

    private final FinancialEntryRepository financialEntryRepository;
    private final ProductionCostRepository productionCostRepository;
    private final PropertyService propertyService;
    private final CropService cropService;
    private final FinanceStatementService financeStatementService;

    public FinanceService(
            FinancialEntryRepository financialEntryRepository,
            ProductionCostRepository productionCostRepository,
            PropertyService propertyService,
            CropService cropService,
            FinanceStatementService financeStatementService
    ) {
        this.financialEntryRepository = financialEntryRepository;
        this.productionCostRepository = productionCostRepository;
        this.propertyService = propertyService;
        this.cropService = cropService;
        this.financeStatementService = financeStatementService;
    }

    @Transactional(readOnly = true)
    public List<FinanceResponse> list(UUID propertyId, UUID cropId) {
        return loadEntries(propertyId, cropId).stream().map(FinanceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FinanceStatementResponse statement(UUID propertyId, UUID cropId) {
        if (propertyId == null && cropId == null) {
            throw new BadRequestException("Informe propertyId ou cropId");
        }
        ResourceScope.require(propertyService, cropService, propertyId, cropId);
        UUID userId = SecurityUtils.getCurrentUserId();
        List<FinancialEntry> entries;
        List<ProductionCost> costs;
        if (cropId != null) {
            entries = financialEntryRepository.findByCropIdAndPropertyUserId(cropId, userId);
            costs = productionCostRepository.findByCropIdAndCropPropertyUserId(cropId, userId);
        } else {
            entries = financialEntryRepository.findByPropertyIdAndPropertyUserId(propertyId, userId);
            costs = productionCostRepository.findByCropPropertyIdAndCropPropertyUserId(propertyId, userId);
        }
        return financeStatementService.compute(entries, costs);
    }

    @Transactional
    public FinanceResponse create(FinanceRequest request) {
        Property property = propertyService.requireOwned(request.propertyId());
        FinancialEntry entry = new FinancialEntry();
        entry.setProperty(property);
        apply(entry, request);
        return FinanceResponse.from(financialEntryRepository.save(entry));
    }

    @Transactional
    public FinanceResponse update(UUID id, FinanceRequest request) {
        FinancialEntry entry = requireOwned(id);
        Property property = propertyService.requireOwned(request.propertyId());
        entry.setProperty(property);
        apply(entry, request);
        return FinanceResponse.from(financialEntryRepository.save(entry));
    }

    @Transactional
    public void delete(UUID id) {
        FinancialEntry entry = requireOwned(id);
        financialEntryRepository.delete(entry);
    }

    private List<FinancialEntry> loadEntries(UUID propertyId, UUID cropId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ResourceScope.require(propertyService, cropService, propertyId, cropId);
        if (propertyId != null && cropId != null) {
            return financialEntryRepository.findByPropertyIdAndCropIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(
                    propertyId, cropId, userId);
        }
        if (cropId != null) {
            return financialEntryRepository.findByCropIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(cropId, userId);
        }
        if (propertyId != null) {
            return financialEntryRepository.findByPropertyIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(propertyId, userId);
        }
        return financialEntryRepository.findByPropertyUserIdOrderByDateDescCreatedAtDesc(userId);
    }

    private FinancialEntry requireOwned(UUID id) {
        return financialEntryRepository.findByIdAndPropertyUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Lançamento financeiro não encontrado"));
    }

    private void apply(FinancialEntry entry, FinanceRequest request) {
        if (request.cropId() != null) {
            Crop crop = cropService.requireOwnedOnProperty(request.cropId(), request.propertyId());
            entry.setCrop(crop);
        } else {
            entry.setCrop(null);
        }
        entry.setType(request.type());
        entry.setCategory(Strings.blankToNull(request.category()));
        entry.setDescription(request.description().trim());
        entry.setAmount(request.amount());
        entry.setDate(request.date());
    }
}
