package com.agrogestao.document;

import com.agrogestao.common.FileNames;
import com.agrogestao.crop.CropService;
import com.agrogestao.document.dto.DocumentResponse;
import com.agrogestao.document.dto.StoredDownload;
import com.agrogestao.domain.Crop;
import com.agrogestao.domain.Document;
import com.agrogestao.domain.Property;
import com.agrogestao.domain.enums.DocumentKind;
import com.agrogestao.exception.BadRequestException;
import com.agrogestao.exception.NotFoundException;
import com.agrogestao.property.PropertyService;
import com.agrogestao.repository.CropRepository;
import com.agrogestao.repository.DocumentRepository;
import com.agrogestao.security.SecurityUtils;
import com.agrogestao.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final CropRepository cropRepository;
    private final PropertyService propertyService;
    private final CropService cropService;
    private final ObjectStorageService objectStorageService;

    public DocumentService(
            DocumentRepository documentRepository,
            CropRepository cropRepository,
            PropertyService propertyService,
            CropService cropService,
            ObjectStorageService objectStorageService
    ) {
        this.documentRepository = documentRepository;
        this.cropRepository = cropRepository;
        this.propertyService = propertyService;
        this.cropService = cropService;
        this.objectStorageService = objectStorageService;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID propertyId) {
        propertyService.requireOwned(propertyId);
        return documentRepository
                .findByPropertyIdAndPropertyUserIdOrderByCreatedAtDesc(propertyId, SecurityUtils.getCurrentUserId())
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentResponse upload(MultipartFile file, UUID propertyId, UUID cropId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo é obrigatório");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Arquivo excede o tamanho máximo de 10MB");
        }
        Property property = propertyService.requireOwned(propertyId);
        Crop crop = cropId == null ? null : cropService.requireOwnedOnProperty(cropId, propertyId);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Não foi possível ler o arquivo enviado");
        }

        String originalName = file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename();
        String sanitized = FileNames.sanitize(originalName);
        String objectKey = "documents/" + property.getId() + "/" + UUID.randomUUID() + "-" + sanitized;
        String contentType = file.getContentType();
        objectStorageService.put(objectKey, bytes, contentType);

        Document document = new Document();
        document.setProperty(property);
        document.setCrop(crop);
        document.setOriginalName(originalName);
        document.setContentType(contentType);
        document.setSizeBytes(bytes.length);
        document.setObjectKey(objectKey);
        document.setKind(DocumentKind.PROPRIEDADE);
        return DocumentResponse.from(documentRepository.save(document));
    }

    public StoredDownload download(UUID id) {
        Document document = requireOwned(id);
        String filename = FileNames.sanitize(document.getOriginalName());
        byte[] data = objectStorageService.get(document.getObjectKey());
        return new StoredDownload(data, filename);
    }

    @Transactional
    public void delete(UUID id) {
        Document document = requireOwned(id);
        String objectKey = document.getObjectKey();
        for (Crop crop : cropRepository.findBySourcePdfId(document.getId())) {
            crop.setSourcePdf(null);
            cropRepository.save(crop);
        }
        documentRepository.delete(document);
        deleteObjectAfterCommit(objectKey);
    }

    private Document requireOwned(UUID id) {
        return documentRepository.findByIdAndPropertyUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Documento não encontrado"));
    }

    private void deleteObjectAfterCommit(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        Runnable delete = () -> {
            try {
                objectStorageService.delete(objectKey);
            } catch (RuntimeException ex) {
                log.warn("Falha ao excluir objeto órfão no armazenamento: {}", objectKey);
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
