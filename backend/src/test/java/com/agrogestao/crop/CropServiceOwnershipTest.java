package com.agrogestao.crop;

import com.agrogestao.common.CascadeDeleteService;
import com.agrogestao.exception.NotFoundException;
import com.agrogestao.property.PropertyService;
import com.agrogestao.repository.CropRepository;
import com.agrogestao.repository.PlannedItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CropServiceOwnershipTest {

    @Mock
    private CropRepository cropRepository;

    @Mock
    private PlannedItemRepository plannedItemRepository;

    @Mock
    private PropertyService propertyService;

    @Mock
    private CascadeDeleteService cascadeDeleteService;

    private CropService cropService;

    private final UUID userA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID userBCropId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        cropService = new CropService(cropRepository, plannedItemRepository, propertyService, cascadeDeleteService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA, "n", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userCannotGetAnotherUsersCrop() {
        when(cropRepository.findByIdAndPropertyUserId(userBCropId, userA)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> cropService.get(userBCropId));

        assertEquals("Cultura não encontrada", exception.getMessage());
        verify(cropRepository).findByIdAndPropertyUserId(userBCropId, userA);
    }

    @Test
    void userCannotDeleteAnotherUsersCrop() {
        when(cropRepository.findByIdAndPropertyUserId(userBCropId, userA)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> cropService.delete(userBCropId));

        assertEquals("Cultura não encontrada", exception.getMessage());
    }
}
