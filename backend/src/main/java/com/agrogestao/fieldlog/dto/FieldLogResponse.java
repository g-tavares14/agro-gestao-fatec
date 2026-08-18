package com.agrogestao.fieldlog.dto;

import com.agrogestao.domain.FieldLog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FieldLogResponse(
        UUID id,
        UUID propertyId,
        UUID cropId,
        LocalDate date,
        String activity,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static FieldLogResponse from(FieldLog log) {
        return new FieldLogResponse(
                log.getId(),
                log.getProperty().getId(),
                log.getCrop() == null ? null : log.getCrop().getId(),
                log.getDate(),
                log.getActivity(),
                log.getNotes(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
