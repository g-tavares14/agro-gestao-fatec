package com.agrogestao.common;

import com.agrogestao.domain.Crop;
import com.agrogestao.domain.Property;
import com.agrogestao.domain.enums.DocumentKind;
import com.agrogestao.repository.CropRepository;
import com.agrogestao.repository.DocumentRepository;
import com.agrogestao.repository.FieldLogRepository;
import com.agrogestao.repository.FinancialEntryRepository;
import com.agrogestao.repository.PlannedItemRepository;
import com.agrogestao.repository.ProductionCostRepository;
import com.agrogestao.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class CascadeDeleteService {

    private static final Logger log = LoggerFactory.getLogger(CascadeDeleteService.class);

    private final CropRepository cropRepository;
    private final PlannedItemRepository plannedItemRepository;
    private final ProductionCostRepository productionCostRepository;
    private final FieldLogRepository fieldLogRepository;
    private final FinancialEntryRepository financialEntryRepository;
    private final DocumentRepository documentRepository;
    private final ObjectStorageService objectStorageService;

    public CascadeDeleteService(
            CropRepository cropRepository,
            PlannedItemRepository plannedItemRepository,
            ProductionCostRepository productionCostRepository,
            FieldLogRepository fieldLogRepository,
            FinancialEntryRepository financialEntryRepository,
            DocumentRepository documentRepository,
            ObjectStorageService objectStorageService
    ) {
        this.cropRepository = cropRepository;
        this.plannedItemRepository = plannedItemRepository;
        this.productionCostRepository = productionCostRepository;
        this.fieldLogRepository = fieldLogRepository;
        this.financialEntryRepository = financialEntryRepository;
        this.documentRepository = documentRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional
    public void deleteCropGraph(Crop crop) {
        List<String> objectKeys = documentRepository.findObjectKeysByCropIdAndKind(
                crop.getId(),
                DocumentKind.CULTURA_PDF
        );
        deleteCropRows(crop);
        deleteObjectsAfterCommit(objectKeys);
    }

    @Transactional
    public void deletePropertyGraph(Property property) {
        List<String> objectKeys = documentRepository.findObjectKeysByPropertyId(property.getId());
        for (Crop crop : cropRepository.findByPropertyId(property.getId())) {
            deleteCropRows(crop);
        }
        fieldLogRepository.deleteByPropertyId(property.getId());
        financialEntryRepository.deleteByPropertyId(property.getId());
        documentRepository.deleteByPropertyId(property.getId());
        deleteObjectsAfterCommit(objectKeys);
    }

    private void deleteCropRows(Crop crop) {
        plannedItemRepository.deleteByCropId(crop.getId());
        productionCostRepository.deleteByCropId(crop.getId());
        fieldLogRepository.clearCropByCropId(crop.getId());
        financialEntryRepository.clearCropByCropId(crop.getId());

        crop.setSourcePdf(null);
        cropRepository.saveAndFlush(crop);

        documentRepository.deleteByCropIdAndKind(crop.getId(), DocumentKind.CULTURA_PDF);
        documentRepository.clearCropByCropId(crop.getId());
        cropRepository.delete(crop);
    }

    private void deleteObjectsAfterCommit(Collection<String> objectKeys) {
        List<String> keys = new ArrayList<>();
        for (String objectKey : objectKeys) {
            if (objectKey != null && !objectKey.isBlank()) {
                keys.add(objectKey);
            }
        }
        if (keys.isEmpty()) {
            return;
        }
        Runnable delete = () -> {
            for (String objectKey : keys) {
                try {
                    objectStorageService.delete(objectKey);
                } catch (RuntimeException ex) {
                    log.warn("Falha ao excluir objeto órfão no armazenamento: {}", objectKey);
                }
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delete.run();
            }
        });
    }
}
