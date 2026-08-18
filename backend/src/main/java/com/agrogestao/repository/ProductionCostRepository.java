package com.agrogestao.repository;

import com.agrogestao.domain.ProductionCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionCostRepository extends JpaRepository<ProductionCost, UUID> {

    Optional<ProductionCost> findByIdAndCropPropertyUserId(UUID id, UUID userId);

    List<ProductionCost> findByCropIdAndCropPropertyUserIdOrderByDateDescCreatedAtDesc(UUID cropId, UUID userId);

    List<ProductionCost> findByCropPropertyIdAndCropPropertyUserId(UUID propertyId, UUID userId);

    List<ProductionCost> findByCropIdAndCropPropertyUserId(UUID cropId, UUID userId);

    List<ProductionCost> findByCropPropertyUserIdAndDateBetween(UUID userId, LocalDate start, LocalDate end);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProductionCost c where c.crop.id = :cropId")
    int deleteByCropId(@Param("cropId") UUID cropId);
}
