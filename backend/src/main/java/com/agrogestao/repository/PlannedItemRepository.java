package com.agrogestao.repository;

import com.agrogestao.domain.PlannedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PlannedItemRepository extends JpaRepository<PlannedItem, UUID> {

    List<PlannedItem> findByCropIdIn(Collection<UUID> cropIds);

    List<PlannedItem> findByCropIdOrderByDescriptionAsc(UUID cropId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PlannedItem p where p.crop.id = :cropId")
    int deleteByCropId(@Param("cropId") UUID cropId);
}
