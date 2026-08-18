package com.agrogestao.crop;

import com.agrogestao.config.GeminiProperties;
import com.agrogestao.crop.dto.CropRequest;
import com.agrogestao.crop.dto.CropResponse;
import com.agrogestao.crop.dto.ItemRequest;
import com.agrogestao.crop.dto.PdfAnalyzeResponse;
import com.agrogestao.crop.dto.PdfConfirmRequest;
import com.agrogestao.crop.dto.PdfExtractedResponse;
import com.agrogestao.domain.Crop;
import com.agrogestao.domain.Document;
import com.agrogestao.domain.PdfAnalysis;
import com.agrogestao.domain.PlannedItem;
import com.agrogestao.domain.Property;
import com.agrogestao.domain.User;
import com.agrogestao.domain.enums.CropOrigin;
import com.agrogestao.domain.enums.CropStatus;
import com.agrogestao.domain.enums.PdfAnalysisStatus;
import com.agrogestao.exception.BadGatewayException;
import com.agrogestao.exception.BadRequestException;
import com.agrogestao.gemini.CropExtraction;
import com.agrogestao.gemini.CropExtractionMapper;
import com.agrogestao.gemini.GeminiCropExtractor;
import com.agrogestao.property.PropertyService;
import com.agrogestao.repository.CropRepository;
import com.agrogestao.repository.DocumentRepository;
import com.agrogestao.repository.PlannedItemRepository;
import com.agrogestao.repository.ProductionCostRepository;
import com.agrogestao.repository.UserRepository;
import com.agrogestao.storage.ObjectStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CropPdfServiceTest {

    @Mock
    private GeminiProperties geminiProperties;

    @Mock
    private GeminiCropExtractor geminiCropExtractor;

    @Mock
    private CropExtractionMapper cropExtractionMapper;

    @Mock
    private PropertyService propertyService;

    @Mock
    private CropService cropService;

    @Mock
    private CropRepository cropRepository;

    @Mock
    private PlannedItemRepository plannedItemRepository;

    @Mock
    private ProductionCostRepository productionCostRepository;

    @Mock
    private PdfAnalysisStore pdfAnalysisStore;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private CropPdfService cropPdfService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID propertyId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID analysisId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID cropId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, "n", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void analyzeIsNotTransactional() throws Exception {
        assertNull(CropPdfService.class.getDeclaredMethod("analyze", MultipartFile.class, UUID.class)
                .getAnnotation(Transactional.class));
        assertNull(CropPdfService.class.getAnnotation(Transactional.class));
    }

    @Test
    void geminiFailurePersistsFailedAndThrowsBadGateway() {
        byte[] pdfBytes = "%PDF-1.4 test".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "ficha.pdf", "application/pdf", pdfBytes);
        User user = new User();
        user.setId(userId);
        PdfAnalysis pending = pendingAnalysis();

        when(geminiProperties.isConfigured()).thenReturn(true);
        when(propertyService.requireOwned(propertyId)).thenReturn(property());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(pdfAnalysisStore.insertPending(user, "ficha.pdf", pdfBytes.length)).thenReturn(pending);
        when(geminiCropExtractor.extractJson(pdfBytes)).thenThrow(new BadGatewayException("Falha no Gemini"));

        BadGatewayException exception = assertThrows(
                BadGatewayException.class,
                () -> cropPdfService.analyze(file, propertyId)
        );

        assertEquals("Falha no Gemini", exception.getMessage());
        verify(objectStorageService).put(pending.getObjectKey(), pdfBytes, "application/pdf");
        verify(pdfAnalysisStore).markFailed(analysisId);
        verify(pdfAnalysisStore, never()).markDone(any(), any(), any());
        verify(cropExtractionMapper, never()).parse(any());
    }

    @Test
    void confirmConsumeIfDoneZeroThrowsAndDoesNotSaveCrop() {
        PdfAnalysis analysis = pendingAnalysis();
        analysis.setStatus(PdfAnalysisStatus.DONE);
        analysis.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(pdfAnalysisStore.consumeIfDone(eq(analysisId), eq(userId), any(Instant.class))).thenReturn(0);
        when(pdfAnalysisStore.findOwned(analysisId, userId)).thenReturn(Optional.of(analysis));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> cropPdfService.confirm(confirmRequest())
        );

        assertEquals("Análise indisponível ou incompleta", exception.getMessage());
        verify(cropService, never()).create(any(Property.class), any(), any());
        verify(cropService, never()).apply(any(), any());
        verify(cropRepository, never()).save(any());
        verify(objectStorageService, never()).copy(any(), any());
        verify(objectStorageService, never()).move(any(), any());
        verify(objectStorageService, never()).put(any(), any(), any());
    }

    @Test
    void confirmSuccessUsesCropServiceCreateAndCopiesAfterConsume() {
        PdfAnalysis analysis = pendingAnalysis();
        analysis.setStatus(PdfAnalysisStatus.DONE);
        analysis.setOriginalName("ficha.pdf");
        analysis.setSizeBytes(2048L);
        Property property = property();
        Crop crop = crop(property);
        CropResponse response = cropResponse();

        when(pdfAnalysisStore.consumeIfDone(eq(analysisId), eq(userId), any(Instant.class))).thenReturn(1);
        when(pdfAnalysisStore.findOwned(analysisId, userId)).thenReturn(Optional.of(analysis));
        when(propertyService.requireOwned(propertyId)).thenReturn(property);
        when(cropService.create(eq(property), eq(CropOrigin.PDF), any(CropRequest.class))).thenReturn(crop);
        when(plannedItemRepository.save(any(PlannedItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
            return document;
        });
        when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cropService.toResponse(eq(crop), any())).thenReturn(response);

        CropResponse result = cropPdfService.confirm(confirmRequest());

        assertEquals(response, result);

        InOrder order = inOrder(pdfAnalysisStore, cropService, objectStorageService);
        order.verify(pdfAnalysisStore).consumeIfDone(eq(analysisId), eq(userId), any(Instant.class));
        order.verify(cropService).create(eq(property), eq(CropOrigin.PDF), any(CropRequest.class));
        order.verify(objectStorageService).copy(eq(analysis.getObjectKey()), any());

        verify(objectStorageService, never()).move(any(), any());
        verify(cropService, never()).apply(any(), any());

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(documentCaptor.capture());
        assertEquals("ficha.pdf", documentCaptor.getValue().getOriginalName());
        assertEquals(2048L, documentCaptor.getValue().getSizeBytes());
    }

    @Test
    void analyzeSuccessMapsEnglishResponse() {
        byte[] pdfBytes = "%PDF-1.4 test".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "ficha.pdf", "application/pdf", pdfBytes);
        User user = new User();
        user.setId(userId);
        PdfAnalysis pending = pendingAnalysis();
        CropExtraction extraction = new CropExtraction(
                "Alface", null, null, null, null, null, null, null, null, List.of("aviso"),
                List.of(), List.of(), List.of(), List.of()
        );
        PdfAnalyzeResponse mapped = new PdfAnalyzeResponse(
                analysisId,
                new PdfExtractedResponse("Alface", null, null, null, null, null, null, List.of(), List.of(), List.of(), List.of()),
                List.of("aviso")
        );

        when(geminiProperties.isConfigured()).thenReturn(true);
        when(propertyService.requireOwned(propertyId)).thenReturn(property());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(pdfAnalysisStore.insertPending(user, "ficha.pdf", pdfBytes.length)).thenReturn(pending);
        when(geminiCropExtractor.extractJson(pdfBytes)).thenReturn("{json}");
        when(cropExtractionMapper.parse("{json}")).thenReturn(extraction);
        when(cropExtractionMapper.toResponse(analysisId, extraction)).thenReturn(mapped);

        PdfAnalyzeResponse result = cropPdfService.analyze(file, propertyId);

        assertEquals(mapped, result);
        verify(objectStorageService).put(pending.getObjectKey(), pdfBytes, "application/pdf");
        verify(pdfAnalysisStore).markDone(eq(analysisId), eq("{json}"), any());
        verify(pdfAnalysisStore, never()).markFailed(any());
    }

    private PdfConfirmRequest confirmRequest() {
        return new PdfConfirmRequest(
                analysisId,
                propertyId,
                "Alface",
                null,
                null,
                null,
                null,
                null,
                CropStatus.PLANEJADA,
                null,
                null,
                List.of(new ItemRequest("Aração", "h/m", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN)),
                List.of(),
                List.of(),
                List.of(),
                false
        );
    }

    private PdfAnalysis pendingAnalysis() {
        PdfAnalysis analysis = new PdfAnalysis();
        analysis.setId(analysisId);
        analysis.setObjectKey("tmp/pdf-analyses/" + userId + "/" + analysisId + ".pdf");
        analysis.setStatus(PdfAnalysisStatus.PENDING);
        return analysis;
    }

    private Property property() {
        Property property = new Property();
        property.setId(propertyId);
        property.setName("Fazenda");
        return property;
    }

    private Crop crop(Property property) {
        Crop crop = new Crop();
        crop.setId(cropId);
        crop.setProperty(property);
        crop.setName("Alface");
        crop.setOrigin(CropOrigin.PDF);
        crop.setStatus(CropStatus.PLANEJADA);
        return crop;
    }

    private CropResponse cropResponse() {
        return new CropResponse(
                cropId,
                propertyId,
                "Fazenda",
                "Alface",
                null,
                null,
                null,
                null,
                null,
                CropStatus.PLANEJADA,
                null,
                null,
                CropOrigin.PDF,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now()
        );
    }
}
