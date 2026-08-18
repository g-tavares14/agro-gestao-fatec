package com.agrogestao.fieldlog;

import com.agrogestao.common.ResourceScope;
import com.agrogestao.common.Strings;
import com.agrogestao.crop.CropService;
import com.agrogestao.domain.Crop;
import com.agrogestao.domain.FieldLog;
import com.agrogestao.domain.Property;
import com.agrogestao.exception.NotFoundException;
import com.agrogestao.fieldlog.dto.FieldLogRequest;
import com.agrogestao.fieldlog.dto.FieldLogResponse;
import com.agrogestao.property.PropertyService;
import com.agrogestao.repository.FieldLogRepository;
import com.agrogestao.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FieldLogService {

    private final FieldLogRepository fieldLogRepository;
    private final PropertyService propertyService;
    private final CropService cropService;

    public FieldLogService(
            FieldLogRepository fieldLogRepository,
            PropertyService propertyService,
            CropService cropService
    ) {
        this.fieldLogRepository = fieldLogRepository;
        this.propertyService = propertyService;
        this.cropService = cropService;
    }

    @Transactional(readOnly = true)
    public List<FieldLogResponse> list(UUID propertyId, UUID cropId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ResourceScope.require(propertyService, cropService, propertyId, cropId);
        List<FieldLog> logs;
        if (propertyId != null && cropId != null) {
            logs = fieldLogRepository.findByPropertyIdAndCropIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(
                    propertyId, cropId, userId);
        } else if (cropId != null) {
            logs = fieldLogRepository.findByCropIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(cropId, userId);
        } else if (propertyId != null) {
            logs = fieldLogRepository.findByPropertyIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(propertyId, userId);
        } else {
            logs = fieldLogRepository.findByPropertyUserIdOrderByDateDescCreatedAtDesc(userId);
        }
        return logs.stream().map(FieldLogResponse::from).toList();
    }

    @Transactional
    public FieldLogResponse create(FieldLogRequest request) {
        Property property = propertyService.requireOwned(request.propertyId());
        FieldLog log = new FieldLog();
        log.setProperty(property);
        apply(log, request);
        return FieldLogResponse.from(fieldLogRepository.save(log));
    }

    @Transactional
    public FieldLogResponse update(UUID id, FieldLogRequest request) {
        FieldLog log = requireOwned(id);
        Property property = propertyService.requireOwned(request.propertyId());
        log.setProperty(property);
        apply(log, request);
        return FieldLogResponse.from(fieldLogRepository.save(log));
    }

    @Transactional
    public void delete(UUID id) {
        FieldLog log = requireOwned(id);
        fieldLogRepository.delete(log);
    }

    private FieldLog requireOwned(UUID id) {
        return fieldLogRepository.findByIdAndPropertyUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Registro de campo não encontrado"));
    }

    private void apply(FieldLog log, FieldLogRequest request) {
        if (request.cropId() != null) {
            Crop crop = cropService.requireOwnedOnProperty(request.cropId(), request.propertyId());
            log.setCrop(crop);
        } else {
            log.setCrop(null);
        }
        log.setDate(request.date());
        log.setActivity(request.activity().trim());
        log.setNotes(Strings.blankToNull(request.notes()));
    }
}
