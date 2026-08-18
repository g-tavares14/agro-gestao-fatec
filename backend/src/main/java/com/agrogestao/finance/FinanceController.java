package com.agrogestao.finance;

import com.agrogestao.finance.dto.FinanceRequest;
import com.agrogestao.finance.dto.FinanceResponse;
import com.agrogestao.finance.dto.FinanceStatementResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping
    public List<FinanceResponse> list(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) UUID cropId
    ) {
        return financeService.list(propertyId, cropId);
    }

    @GetMapping("/statement")
    public FinanceStatementResponse statement(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) UUID cropId
    ) {
        return financeService.statement(propertyId, cropId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FinanceResponse create(@Valid @RequestBody FinanceRequest request) {
        return financeService.create(request);
    }

    @PutMapping("/{id}")
    public FinanceResponse update(@PathVariable UUID id, @Valid @RequestBody FinanceRequest request) {
        return financeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        financeService.delete(id);
    }
}
