package com.agrogestao.crop;

import com.agrogestao.crop.dto.CropCycleRequest;
import com.agrogestao.crop.dto.CropRequest;
import com.agrogestao.crop.dto.CropResponse;
import com.agrogestao.crop.dto.PdfAnalyzeResponse;
import com.agrogestao.crop.dto.PdfConfirmRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crops")
public class CropController {

    private final CropService cropService;
    private final CropPdfService cropPdfService;

    public CropController(CropService cropService, CropPdfService cropPdfService) {
        this.cropService = cropService;
        this.cropPdfService = cropPdfService;
    }

    @GetMapping
    public List<CropResponse> list(@RequestParam UUID propertyId) {
        return cropService.list(propertyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CropResponse create(@Valid @RequestBody CropRequest request) {
        return cropService.create(request);
    }

    @PostMapping(value = "/pdf/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PdfAnalyzeResponse analyze(
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID propertyId
    ) {
        return cropPdfService.analyze(file, propertyId);
    }

    @PostMapping("/pdf/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public CropResponse confirm(@Valid @RequestBody PdfConfirmRequest request) {
        return cropPdfService.confirm(request);
    }

    @GetMapping("/{id}")
    public CropResponse get(@PathVariable UUID id) {
        return cropService.get(id);
    }

    @PutMapping("/{id}")
    public CropResponse update(@PathVariable UUID id, @Valid @RequestBody CropRequest request) {
        return cropService.update(id, request);
    }

    @PatchMapping("/{id}/cycle")
    public CropResponse updateCycle(@PathVariable UUID id, @Valid @RequestBody CropCycleRequest request) {
        return cropService.updateCycle(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        cropService.delete(id);
    }
}
