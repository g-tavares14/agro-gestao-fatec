package com.agrogestao.repository;

import com.agrogestao.domain.PdfAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PdfAnalysisRepository extends JpaRepository<PdfAnalysis, UUID> {

    Optional<PdfAnalysis> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PdfAnalysis a set a.status = com.agrogestao.domain.enums.PdfAnalysisStatus.CONSUMED where a.id = :id and a.user.id = :userId and a.status = com.agrogestao.domain.enums.PdfAnalysisStatus.DONE and (a.expiresAt is null or a.expiresAt > :now)")
    int consumeIfDone(@Param("id") UUID id, @Param("userId") UUID userId, @Param("now") Instant now);
}
