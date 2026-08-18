package com.agrogestao.repository;

import com.agrogestao.domain.Crop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CropRepository extends JpaRepository<Crop, UUID> {

    List<Crop> findByPropertyId(UUID propertyId);

    List<Crop> findByPropertyUserIdOrderByNameAsc(UUID userId);

    List<Crop> findByPropertyIdAndPropertyUserIdOrderByNameAsc(UUID propertyId, UUID userId);

    Optional<Crop> findByIdAndPropertyUserId(UUID id, UUID userId);

    List<Crop> findBySourcePdfId(UUID sourcePdfId);
}
