package com.agrogestao.crop;

import com.agrogestao.common.AppClock;
import com.agrogestao.domain.PdfAnalysis;
import com.agrogestao.domain.User;
import com.agrogestao.domain.enums.PdfAnalysisStatus;
import com.agrogestao.exception.NotFoundException;
import com.agrogestao.repository.PdfAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class PdfAnalysisStore {

    private final PdfAnalysisRepository pdfAnalysisRepository;

    public PdfAnalysisStore(PdfAnalysisRepository pdfAnalysisRepository) {
        this.pdfAnalysisRepository = pdfAnalysisRepository;
    }

    @Transactional
    public PdfAnalysis insertPending(User user, String originalName, long sizeBytes) {
        PdfAnalysis analysis = new PdfAnalysis();
        analysis.setUser(user);
        analysis.setObjectKey("tmp/pdf-analyses/" + user.getId() + "/pending.pdf");
        analysis.setStatus(PdfAnalysisStatus.PENDING);
        analysis.setOriginalName(originalName);
        analysis.setSizeBytes(sizeBytes);
        analysis.setExpiresAt(AppClock.now().plus(24, ChronoUnit.HOURS));
        analysis = pdfAnalysisRepository.save(analysis);
        analysis.setObjectKey("tmp/pdf-analyses/" + user.getId() + "/" + analysis.getId() + ".pdf");
        return pdfAnalysisRepository.save(analysis);
    }

    @Transactional
    public void markDone(UUID id, String extractedJson, String warnings) {
        PdfAnalysis analysis = requireById(id);
        analysis.setStatus(PdfAnalysisStatus.DONE);
        analysis.setExtractedJson(extractedJson);
        analysis.setWarnings(warnings);
        pdfAnalysisRepository.save(analysis);
    }

    @Transactional
    public void markFailed(UUID id) {
        PdfAnalysis analysis = requireById(id);
        analysis.setStatus(PdfAnalysisStatus.FAILED);
        pdfAnalysisRepository.save(analysis);
    }

    @Transactional
    public int consumeIfDone(UUID id, UUID userId, Instant now) {
        return pdfAnalysisRepository.consumeIfDone(id, userId, now);
    }

    @Transactional(readOnly = true)
    public Optional<PdfAnalysis> findOwned(UUID id, UUID userId) {
        return pdfAnalysisRepository.findByIdAndUserId(id, userId);
    }

    private PdfAnalysis requireById(UUID id) {
        return pdfAnalysisRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Análise não encontrada"));
    }
}
