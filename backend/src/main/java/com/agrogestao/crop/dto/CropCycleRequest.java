package com.agrogestao.crop.dto;

import com.agrogestao.domain.enums.CropStatus;

import java.time.LocalDate;

public record CropCycleRequest(
        CropStatus status,
        LocalDate plantingDate,
        LocalDate expectedHarvestDate,
        String notes
) {
}
