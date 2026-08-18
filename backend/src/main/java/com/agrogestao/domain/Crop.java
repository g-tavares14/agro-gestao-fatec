package com.agrogestao.domain;

import com.agrogestao.domain.enums.CropOrigin;
import com.agrogestao.domain.enums.CropStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "crops", indexes = {
        @Index(name = "idx_crops_property_id", columnList = "property_id"),
        @Index(name = "idx_crops_source_pdf_id", columnList = "source_pdf_id")
})
public class Crop extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private String name;

    private String variety;

    @Column(name = "irrigation_system")
    private String irrigationSystem;

    @Column(name = "area_ha", precision = 19, scale = 4)
    private BigDecimal areaHa;

    @Column(name = "planting_date")
    private LocalDate plantingDate;

    @Column(name = "expected_harvest_date")
    private LocalDate expectedHarvestDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CropStatus status;

    @Column(name = "expected_yield")
    private String expectedYield;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CropOrigin origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_pdf_id")
    private Document sourcePdf;

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getIrrigationSystem() {
        return irrigationSystem;
    }

    public void setIrrigationSystem(String irrigationSystem) {
        this.irrigationSystem = irrigationSystem;
    }

    public BigDecimal getAreaHa() {
        return areaHa;
    }

    public void setAreaHa(BigDecimal areaHa) {
        this.areaHa = areaHa;
    }

    public LocalDate getPlantingDate() {
        return plantingDate;
    }

    public void setPlantingDate(LocalDate plantingDate) {
        this.plantingDate = plantingDate;
    }

    public LocalDate getExpectedHarvestDate() {
        return expectedHarvestDate;
    }

    public void setExpectedHarvestDate(LocalDate expectedHarvestDate) {
        this.expectedHarvestDate = expectedHarvestDate;
    }

    public CropStatus getStatus() {
        return status;
    }

    public void setStatus(CropStatus status) {
        this.status = status;
    }

    public String getExpectedYield() {
        return expectedYield;
    }

    public void setExpectedYield(String expectedYield) {
        this.expectedYield = expectedYield;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public CropOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(CropOrigin origin) {
        this.origin = origin;
    }

    public Document getSourcePdf() {
        return sourcePdf;
    }

    public void setSourcePdf(Document sourcePdf) {
        this.sourcePdf = sourcePdf;
    }
}
