package com.agrogestao.domain.enums;

public enum CropStatus {
    PLANEJADA,
    PLANTADA,
    EM_DESENVOLVIMENTO,
    COLHIDA,
    ENCERRADA;

    public boolean isActive() {
        return this != COLHIDA && this != ENCERRADA;
    }
}
