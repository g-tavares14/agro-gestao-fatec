package com.agrogestao.repository;

import com.agrogestao.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    List<Property> findByUserIdOrderByNameAsc(UUID userId);

    Optional<Property> findByIdAndUserId(UUID id, UUID userId);
}
