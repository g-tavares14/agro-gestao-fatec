package com.agrogestao.crop;

import com.agrogestao.common.AppClock;
import com.agrogestao.common.Strings;
import com.agrogestao.config.GeminiProperties;
import com.agrogestao.crop.dto.CropRequest;
import com.agrogestao.crop.dto.CropResponse;
import com.agrogestao.crop.dto.ItemRequest;
import com.agrogestao.crop.dto.PdfAnalyzeResponse;
import com.agrogestao.crop.dto.PdfConfirmRequest;
import com.agrogestao.domain.Crop;
import com.agrogestao.domain.Document;
import com.agrogestao.domain.PdfAnalysis;
import com.agrogestao.domain.PlannedItem;
import com.agrogestao.domain.ProductionCost;
import com.agrogestao.domain.Property;
import com.agrogestao.domain.User;
import com.agrogestao.domain.enums.CropOrigin;
import com.agrogestao.domain.enums.DocumentKind;
import com.agrogestao.domain.enums.ItemCategory;
import com.agrogestao.exception.BadGatewayException;
import com.agrogestao.exception.BadRequestException;
import com.agrogestao.exception.NotFoundException;
import com.agrogestao.exception.ServiceUnavailableException;
import com.agrogestao.exception.UnauthorizedException;
import com.agrogestao.gemini.CropExtraction;
import com.agrogestao.gemini.CropExtractionMapper;
import com.agrogestao.gemini.GeminiCropExtractor;
import com.agrogestao.gemini.UnsupportedGeminiCropExtractor;
import com.agrogestao.property.PropertyService;
import com.agrogestao.repository.CropRepository;
import com.agrogestao.repository.DocumentRepository;
import com.agrogestao.repository.PlannedItemRepository;
import com.agrogestao.repository.ProductionCostRepository;
import com.agrogestao.repository.UserRepository;
import com.agrogestao.security.SecurityUtils;
import com.agrogestao.storage.ObjectStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CropPdfService {

    private static final Logger log = LoggerFactory.getLogger(CropPdfService.class);
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final byte[] PDF_MAGIC = new byte[] {'%', 'P', 'D', 'F'};

    private final GeminiProperties geminiProperties;
    private final GeminiCropExtractor geminiCropExtractor;
    private final CropExtractionMapper cropExtractionMapper;
    private final PropertyService propertyService;
    private final CropService cropService;
    private final CropRepository cropRepository;
    private final PlannedItemRepository plannedItemRepository;
    private final ProductionCostRepository productionCostRepository;
    private final PdfAnalysisStore pdfAnalysisStore;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public CropPdfService(
            GeminiProperties geminiProperties,
            GeminiCropExtractor geminiCropExtractor,
            CropExtractionMapper cropExtractionMapper,
            PropertyService propertyService,
            CropService cropService,
            CropRepository cropRepository,
            PlannedItemRepository plannedItemRepository,
            ProductionCostRepository productionCostRepository,
            PdfAnalysisStore pdfAnalysisStore,
            DocumentRepository documentRepository,
            UserRepository userRepository,
            ObjectStorageService objectStorageService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.geminiProperties = geminiProperties;
        this.geminiCropExtractor = geminiCropExtractor;
        this.cropExtractionMapper = cropExtractionMapper;
        this.propertyService = propertyService;
        this.cropService = cropService;
        this.cropRepository = cropRepository;
        this.plannedItemRepository = plannedItemRepository;
        this.productionCostRepository = productionCostRepository;
        this.pdfAnalysisStore = pdfAnalysisStore;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.objectStorageService = objectStorageService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PdfAnalyzeResponse analyze(MultipartFile file, UUID propertyId) {
        propertyService.requireOwned(propertyId);
        if (!geminiProperties.isConfigured()) {
            throw new ServiceUnavailableException(UnsupportedGeminiCropExtractor.UNAVAILABLE_MESSAGE);
        }

        byte[] pdfBytes = readAndValidatePdf(file);
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Autenticação necessária"));

        PdfAnalysis analysis = pdfAnalysisStore.insertPending(
                user,
                Strings.blankToNull(file.getOriginalFilename()),
                pdfBytes.length
        );
        objectStorageService.put(analysis.getObjectKey(), pdfBytes, "application/pdf");

        try {
            String extractedJson = geminiCropExtractor.extractJson(pdfBytes);
            CropExtraction extracted = cropExtractionMapper.parse(extractedJson);
            pdfAnalysisStore.markDone(analysis.getId(), extractedJson, writeWarnings(extracted.warnings()));
            return cropExtractionMapper.toResponse(analysis.getId(), extracted);
        } catch (RuntimeException ex) {
            pdfAnalysisStore.markFailed(analysis.getId());
            log.warn("Falha ao analisar PDF de cultura");
            if (ex instanceof BadGatewayException || ex instanceof ServiceUnavailableException) {
                throw ex;
            }
            throw new BadGatewayException("Não foi possível analisar o PDF. Tente novamente mais tarde.");
        }
    }

    public CropResponse confirm(PdfConfirmRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Instant now = AppClock.now();
        PdfAnalysis analysis = pdfAnalysisStore.findOwned(request.analysisId(), userId)
                .orElseThrow(() -> new NotFoundException("Análise não encontrada"));
        String targetKey = "documents/" + request.propertyId() + "/" + UUID.randomUUID() + ".pdf";
        CropResponse response = transactionTemplate.execute(status -> {
            if (pdfAnalysisStore.consumeIfDone(request.analysisId(), userId, now) == 0) {
                throwUnavailable(request.analysisId(), userId, now);
            }
            return persistConfirmed(request, analysis, targetKey);
        });
        objectStorageService.copy(analysis.getObjectKey(), targetKey);
        return response;
    }

    private CropResponse persistConfirmed(PdfConfirmRequest request, PdfAnalysis analysis, String targetKey) {
        Property property = propertyService.requireOwned(request.propertyId());
        Crop crop = cropService.create(property, CropOrigin.PDF, toCropRequest(request));

        List<PlannedItem> plannedItems = new ArrayList<>();
        plannedItems.addAll(createItems(crop, request.acoesMecanicas(), ItemCategory.ACAO_MECANICA, request.importCosts()));
        plannedItems.addAll(createItems(crop, request.acoesManuais(), ItemCategory.ACAO_MANUAL, request.importCosts()));
        plannedItems.addAll(createItems(crop, request.insumos(), ItemCategory.INSUMO, request.importCosts()));
        plannedItems.addAll(createItems(crop, request.outros(), ItemCategory.OUTRO, request.importCosts()));

        Document document = new Document();
        document.setProperty(property);
        document.setCrop(crop);
        String originalName = Strings.blankToNull(analysis.getOriginalName());
        document.setOriginalName(originalName == null ? "cultura.pdf" : originalName);
        document.setContentType("application/pdf");
        document.setSizeBytes(analysis.getSizeBytes());
        document.setObjectKey(targetKey);
        document.setKind(DocumentKind.CULTURA_PDF);
        document = documentRepository.save(document);

        crop.setSourcePdf(document);
        crop = cropRepository.save(crop);
        return cropService.toResponse(crop, plannedItems);
    }

    private void throwUnavailable(UUID analysisId, UUID userId, Instant now) {
        PdfAnalysis analysis = pdfAnalysisStore.findOwned(analysisId, userId)
                .orElseThrow(() -> new NotFoundException("Análise não encontrada"));
        if (analysis.getExpiresAt() != null && !analysis.getExpiresAt().isAfter(now)) {
            throw new BadRequestException("Análise expirada. Envie o PDF novamente.");
        }
        throw new BadRequestException("Análise indisponível ou incompleta");
    }

    private List<PlannedItem> createItems(
            Crop crop,
            List<ItemRequest> requests,
            ItemCategory category,
            boolean importCosts
    ) {
        List<PlannedItem> created = new ArrayList<>();
        for (ItemRequest request : requests) {
            if (request == null || request.description() == null || request.description().isBlank()) {
                continue;
            }
            PlannedItem item = new PlannedItem();
            item.setCrop(crop);
            item.setDescription(request.description().trim());
            item.setUnit(Strings.blankToNull(request.unit()));
            item.setQuantity(request.quantity());
            item.setUnitValue(request.unitValue());
            item.setTotalValue(request.totalValue());
            item.setCategory(category);
            item = plannedItemRepository.save(item);
            created.add(item);

            if (importCosts && request.totalValue() != null) {
                ProductionCost cost = new ProductionCost();
                cost.setCrop(crop);
                cost.setDescription(item.getDescription());
                cost.setCategory(category);
                cost.setAmount(request.totalValue());
                cost.setDate(AppClock.today());
                cost.setOrigin(CropOrigin.PDF);
                productionCostRepository.save(cost);
            }
        }
        return created;
    }

    private byte[] readAndValidatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo PDF é obrigatório");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Arquivo excede o tamanho máximo de 10MB");
        }
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean pdfType = contentType != null && contentType.equalsIgnoreCase("application/pdf");
        boolean pdfName = filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");
        if (!pdfType && !pdfName) {
            throw new BadRequestException("O arquivo deve ser um PDF");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Não foi possível ler o arquivo enviado");
        }
        if (bytes.length < PDF_MAGIC.length || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
            throw new BadRequestException("O arquivo não é um PDF válido");
        }
        return bytes;
    }

    private String writeWarnings(List<String> warnings) {
        try {
            return objectMapper.writeValueAsString(warnings == null ? List.of() : warnings);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private static CropRequest toCropRequest(PdfConfirmRequest request) {
        return new CropRequest(
                request.propertyId(),
                request.name(),
                request.variety(),
                request.irrigationSystem(),
                request.areaHa(),
                request.plantingDate(),
                request.expectedHarvestDate(),
                request.status(),
                request.expectedYield(),
                request.notes()
        );
    }
}
