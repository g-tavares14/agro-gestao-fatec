package com.agrogestao.repository;

import com.agrogestao.domain.FieldLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldLogRepository extends JpaRepository<FieldLog, UUID> {

    Optional<FieldLog> findByIdAndPropertyUserId(UUID id, UUID userId);

    List<FieldLog> findByPropertyUserIdOrderByDateDescCreatedAtDesc(UUID userId);

    List<FieldLog> findByPropertyIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(UUID propertyId, UUID userId);

    List<FieldLog> findByCropIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(UUID cropId, UUID userId);

    List<FieldLog> findByPropertyIdAndCropIdAndPropertyUserIdOrderByDateDescCreatedAtDesc(
            UUID propertyId,
            UUID cropId,
            UUID userId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FieldLog f set f.crop = null where f.crop.id = :cropId")
    int clearCropByCropId(@Param("cropId") UUID cropId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FieldLog f where f.property.id = :propertyId")
    int deleteByPropertyId(@Param("propertyId") UUID propertyId);
}
