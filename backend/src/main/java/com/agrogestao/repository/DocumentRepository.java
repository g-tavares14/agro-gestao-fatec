package com.agrogestao.repository;

import com.agrogestao.domain.Document;
import com.agrogestao.domain.enums.DocumentKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndPropertyUserId(UUID id, UUID userId);

    List<Document> findByPropertyIdAndPropertyUserIdOrderByCreatedAtDesc(UUID propertyId, UUID userId);

    @Query("select d.objectKey from Document d where d.crop.id = :cropId and d.kind = :kind")
    List<String> findObjectKeysByCropIdAndKind(@Param("cropId") UUID cropId, @Param("kind") DocumentKind kind);

    @Query("select d.objectKey from Document d where d.property.id = :propertyId")
    List<String> findObjectKeysByPropertyId(@Param("propertyId") UUID propertyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Document d where d.crop.id = :cropId and d.kind = :kind")
    int deleteByCropIdAndKind(@Param("cropId") UUID cropId, @Param("kind") DocumentKind kind);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Document d set d.crop = null where d.crop.id = :cropId")
    int clearCropByCropId(@Param("cropId") UUID cropId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Document d where d.property.id = :propertyId")
    int deleteByPropertyId(@Param("propertyId") UUID propertyId);
}
