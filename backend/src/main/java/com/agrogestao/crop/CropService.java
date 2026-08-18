package com.agrogestao.crop;

import com.agrogestao.common.CascadeDeleteService;
import com.agrogestao.common.Strings;
import com.agrogestao.crop.dto.CropCycleRequest;
import com.agrogestao.crop.dto.CropRequest;
import com.agrogestao.crop.dto.CropResponse;
import com.agrogestao.crop.dto.ItemResponse;
import com.agrogestao.domain.Crop;
import com.agrogestao.domain.PlannedItem;
import com.agrogestao.domain.Property;
import com.agrogestao.domain.enums.CropOrigin;
import com.agrogestao.domain.enums.CropStatus;
import com.agrogestao.domain.enums.ItemCategory;
import com.agrogestao.exception.BadRequestException;
import com.agrogestao.exception.NotFoundException;
import com.agrogestao.property.PropertyService;
import com.agrogestao.repository.CropRepository;
import com.agrogestao.repository.PlannedItemRepository;
import com.agrogestao.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CropService {

    private final CropRepository cropRepository;
    private final PlannedItemRepository plannedItemRepository;
    private final PropertyService propertyService;
    private final CascadeDeleteService cascadeDeleteService;

    public CropService(
            CropRepository cropRepository,
            PlannedItemRepository plannedItemRepository,
            PropertyService propertyService,
            CascadeDeleteService cascadeDeleteService
    ) {
        this.cropRepository = cropRepository;
        this.plannedItemRepository = plannedItemRepository;
        this.propertyService = propertyService;
        this.cascadeDeleteService = cascadeDeleteService;
    }

    @Transactional(readOnly = true)
    public Crop requireOwned(UUID id) {
        return cropRepository.findByIdAndPropertyUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Cultura não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<CropResponse> list(UUID propertyId) {
        propertyService.requireOwned(propertyId);
        UUID userId = SecurityUtils.getCurrentUserId();
        List<Crop> crops = cropRepository.findByPropertyIdAndPropertyUserIdOrderByNameAsc(propertyId, userId);
        return toResponses(crops);
    }

    @Transactional(readOnly = true)
    public CropResponse get(UUID id) {
        Crop crop = requireOwned(id);
        return toResponse(crop, plannedItemRepository.findByCropIdOrderByDescriptionAsc(crop.getId()));
    }

    @Transactional
    public CropResponse create(CropRequest request) {
        if (request.propertyId() == null) {
            throw new BadRequestException("Campo obrigatório");
        }
        Property property = propertyService.requireOwned(request.propertyId());
        Crop crop = create(property, CropOrigin.MANUAL, request);
        return toResponse(crop, List.of());
    }

    public Crop create(Property property, CropOrigin origin, CropRequest request) {
        Crop crop = new Crop();
        crop.setProperty(property);
        crop.setOrigin(origin);
        apply(crop, request);
        return cropRepository.save(crop);
    }

    @Transactional
    public CropResponse update(UUID id, CropRequest request) {
        Crop crop = requireOwned(id);
        apply(crop, request);
        crop = cropRepository.save(crop);
        return toResponse(crop, plannedItemRepository.findByCropIdOrderByDescriptionAsc(crop.getId()));
    }

    @Transactional
    public CropResponse updateCycle(UUID id, CropCycleRequest request) {
        Crop crop = requireOwned(id);
        if (request.status() != null) {
            crop.setStatus(request.status());
        }
        crop.setPlantingDate(request.plantingDate());
        crop.setExpectedHarvestDate(request.expectedHarvestDate());
        crop.setNotes(Strings.blankToNull(request.notes()));
        crop = cropRepository.save(crop);
        return toResponse(crop, plannedItemRepository.findByCropIdOrderByDescriptionAsc(crop.getId()));
    }

    @Transactional
    public void delete(UUID id) {
        Crop crop = requireOwned(id);
        cascadeDeleteService.deleteCropGraph(crop);
    }

    @Transactional(readOnly = true)
    public Crop requireOwnedOnProperty(UUID cropId, UUID propertyId) {
        Crop crop = requireOwned(cropId);
        if (!crop.getProperty().getId().equals(propertyId)) {
            throw new NotFoundException("Cultura não encontrada");
        }
        return crop;
    }

    public void apply(Crop crop, CropRequest request) {
        crop.setName(request.name().trim());
        crop.setVariety(Strings.blankToNull(request.variety()));
        crop.setIrrigationSystem(Strings.blankToNull(request.irrigationSystem()));
        crop.setAreaHa(request.areaHa());
        crop.setPlantingDate(request.plantingDate());
        crop.setExpectedHarvestDate(request.expectedHarvestDate());
        crop.setStatus(request.status() == null ? CropStatus.PLANEJADA : request.status());
        crop.setExpectedYield(Strings.blankToNull(request.expectedYield()));
        crop.setNotes(Strings.blankToNull(request.notes()));
    }

    private List<CropResponse> toResponses(List<Crop> crops) {
        if (crops.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = crops.stream().map(Crop::getId).toList();
        Map<UUID, List<PlannedItem>> itemsByCrop = plannedItemRepository.findByCropIdIn(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getCrop().getId()));
        List<CropResponse> responses = new ArrayList<>(crops.size());
        for (Crop crop : crops) {
            responses.add(toResponse(crop, itemsByCrop.getOrDefault(crop.getId(), List.of())));
        }
        return responses;
    }

    CropResponse toResponse(Crop crop, List<PlannedItem> items) {
        Map<ItemCategory, List<ItemResponse>> grouped = new EnumMap<>(ItemCategory.class);
        for (ItemCategory category : ItemCategory.values()) {
            grouped.put(category, new ArrayList<>());
        }
        for (PlannedItem item : items) {
            grouped.computeIfAbsent(item.getCategory(), key -> new ArrayList<>()).add(ItemResponse.from(item));
        }
        return CropResponse.from(
                crop,
                grouped.get(ItemCategory.ACAO_MECANICA),
                grouped.get(ItemCategory.ACAO_MANUAL),
                grouped.get(ItemCategory.INSUMO),
                grouped.get(ItemCategory.OUTRO)
        );
    }
}
