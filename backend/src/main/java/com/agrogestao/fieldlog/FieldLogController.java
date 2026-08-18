package com.agrogestao.fieldlog;

import com.agrogestao.fieldlog.dto.FieldLogRequest;
import com.agrogestao.fieldlog.dto.FieldLogResponse;
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
@RequestMapping("/api/field-logs")
public class FieldLogController {

    private final FieldLogService fieldLogService;

    public FieldLogController(FieldLogService fieldLogService) {
        this.fieldLogService = fieldLogService;
    }

    @GetMapping
    public List<FieldLogResponse> list(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) UUID cropId
    ) {
        return fieldLogService.list(propertyId, cropId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FieldLogResponse create(@Valid @RequestBody FieldLogRequest request) {
        return fieldLogService.create(request);
    }

    @PutMapping("/{id}")
    public FieldLogResponse update(@PathVariable UUID id, @Valid @RequestBody FieldLogRequest request) {
        return fieldLogService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fieldLogService.delete(id);
    }
}
