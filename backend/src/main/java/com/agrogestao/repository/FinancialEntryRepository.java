package com.agrogestao.repository;

import com.agrogestao.domain.FinancialEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialEntryRepository extends JpaRepository<FinancialEntry, UUID> {

    Optional<FinancialEntry> findByIdAndPropertyUserId(UUID id, UUID userId);

    List<FinancialEntry> findByPropertyUserIdOrderByDateDescCreatedAtDesc(UUID userId);

    List<FinancialEntry> findByPropertyIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(UUID propertyId, UUID userId);

    List<FinancialEntry> findByCropIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(UUID cropId, UUID userId);

    List<FinancialEntry> findByPropertyIdAndCropIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(
            UUID propertyId,
            UUID cropId,
            UUID userId
    );

    List<FinancialEntry> findByPropertyUserIdAndDateBetween(UUID userId, LocalDate start, LocalDate end);

    List<FinancialEntry> findByPropertyIdAndPropertyUserId(UUID propertyId, UUID userId);

    List<FinancialEntry> findByCropIdAndPropertyUserId(UUID cropId, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FinancialEntry e set e.crop = null where e.crop.id = :cropId")
    int clearCropByCropId(@Param("cropId") UUID cropId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FinancialEntry e where e.property.id = :propertyId")
    int deleteByPropertyId(@Param("propertyId") UUID propertyId);
}
