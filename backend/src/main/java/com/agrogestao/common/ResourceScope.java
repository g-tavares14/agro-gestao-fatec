package com.agrogestao.common;

import com.agrogestao.crop.CropService;
import com.agrogestao.property.PropertyService;

import java.util.UUID;

public final class ResourceScope {

    private ResourceScope() {
    }

    public static void require(
            PropertyService properties,
            CropService crops,
            UUID propertyId,
            UUID cropId
    ) {
        if (propertyId != null && cropId != null) {
            properties.requireOwned(propertyId);
            crops.requireOwnedOnProperty(cropId, propertyId);
            return;
        }
        if (cropId != null) {
            crops.requireOwned(cropId);
            return;
        }
        if (propertyId != null) {
            properties.requireOwned(propertyId);
        }
    }
}
