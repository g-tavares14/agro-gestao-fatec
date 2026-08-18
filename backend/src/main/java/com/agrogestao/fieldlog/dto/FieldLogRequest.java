package com.agrogestao.fieldlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record FieldLogRequest(
        @NotNull(message = "Campo obrigatório")
        UUID propertyId,

        UUID cropId,

        @NotNull(message = "Campo obrigatório")
        LocalDate date,

        @NotBlank(message = "Campo obrigatório")
        String activity,

        String notes
) {
}
