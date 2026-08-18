package com.agrogestao.cost;

import com.agrogestao.cost.dto.CostRequest;
import com.agrogestao.cost.dto.CostResponse;
import com.agrogestao.crop.CropService;
import com.agrogestao.domain.Crop;
import com.agrogestao.domain.ProductionCost;
import com.agrogestao.domain.enums.CropOrigin;
import com.agrogestao.exception.NotFoundException;
import com.agrogestao.repository.ProductionCostRepository;
import com.agrogestao.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductionCostService {

    private final ProductionCostRepository productionCostRepository;
    private final CropService cropService;

    public ProductionCostService(ProductionCostRepository productionCostRepository, CropService cropService) {
        this.productionCostRepository = productionCostRepository;
        this.cropService = cropService;
    }

    @Transactional(readOnly = true)
    public List<CostResponse> list(UUID cropId) {
        cropService.requireOwned(cropId);
        return productionCostRepository
                .findByCropIdAndCropPropertyUserIdOrderByDateDescCreatedAtDesc(cropId, SecurityUtils.getCurrentUserId())
                .stream()
                .map(CostResponse::from)
                .toList();
    }

    @Transactional
    public CostResponse create(UUID cropId, CostRequest request) {
        Crop crop = cropService.requireOwned(cropId);
        ProductionCost cost = new ProductionCost();
        cost.setCrop(crop);
        cost.setOrigin(CropOrigin.MANUAL);
        apply(cost, request);
        return CostResponse.from(productionCostRepository.save(cost));
    }

    @Transactional
    public CostResponse update(UUID id, CostRequest request) {
        ProductionCost cost = requireOwned(id);
        apply(cost, request);
        return CostResponse.from(productionCostRepository.save(cost));
    }

    @Transactional
    public void delete(UUID id) {
        ProductionCost cost = requireOwned(id);
        productionCostRepository.delete(cost);
    }

    private ProductionCost requireOwned(UUID id) {
        return productionCostRepository.findByIdAndCropPropertyUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Custo não encontrado"));
    }

    private static void apply(ProductionCost cost, CostRequest request) {
        cost.setDescription(request.description().trim());
        cost.setCategory(request.category());
        cost.setAmount(request.amount());
        cost.setDate(request.date());
    }
}
