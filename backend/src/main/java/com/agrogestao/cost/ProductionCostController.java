package com.agrogestao.cost;

import com.agrogestao.cost.dto.CostRequest;
import com.agrogestao.cost.dto.CostResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ProductionCostController {

    private final ProductionCostService productionCostService;

    public ProductionCostController(ProductionCostService productionCostService) {
        this.productionCostService = productionCostService;
    }

    @GetMapping("/api/crops/{cropId}/costs")
    public List<CostResponse> list(@PathVariable UUID cropId) {
        return productionCostService.list(cropId);
    }

    @PostMapping("/api/crops/{cropId}/costs")
    @ResponseStatus(HttpStatus.CREATED)
    public CostResponse create(@PathVariable UUID cropId, @Valid @RequestBody CostRequest request) {
        return productionCostService.create(cropId, request);
    }

    @PutMapping("/api/costs/{id}")
    public CostResponse update(@PathVariable UUID id, @Valid @RequestBody CostRequest request) {
        return productionCostService.update(id, request);
    }

    @DeleteMapping("/api/costs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productionCostService.delete(id);
    }
}
