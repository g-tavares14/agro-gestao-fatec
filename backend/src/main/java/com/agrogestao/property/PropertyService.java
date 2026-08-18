package com.agrogestao.property;

import com.agrogestao.common.CascadeDeleteService;
import com.agrogestao.common.Strings;
import com.agrogestao.domain.Property;
import com.agrogestao.domain.User;
import com.agrogestao.exception.NotFoundException;
import com.agrogestao.exception.UnauthorizedException;
import com.agrogestao.property.dto.PropertyRequest;
import com.agrogestao.property.dto.PropertyResponse;
import com.agrogestao.repository.PropertyRepository;
import com.agrogestao.repository.UserRepository;
import com.agrogestao.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CascadeDeleteService cascadeDeleteService;

    public PropertyService(
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            CascadeDeleteService cascadeDeleteService
    ) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.cascadeDeleteService = cascadeDeleteService;
    }

    @Transactional(readOnly = true)
    public Property requireOwned(UUID id) {
        return propertyRepository.findByIdAndUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> list() {
        return propertyRepository.findByUserIdOrderByNameAsc(SecurityUtils.getCurrentUserId()).stream()
                .map(PropertyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(UUID id) {
        return PropertyResponse.from(requireOwned(id));
    }

    @Transactional
    public PropertyResponse create(PropertyRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Autenticação necessária"));
        Property property = new Property();
        property.setUser(user);
        apply(property, request);
        return PropertyResponse.from(propertyRepository.save(property));
    }

    @Transactional
    public PropertyResponse update(UUID id, PropertyRequest request) {
        Property property = requireOwned(id);
        apply(property, request);
        return PropertyResponse.from(propertyRepository.save(property));
    }

    @Transactional
    public void delete(UUID id) {
        Property property = requireOwned(id);
        cascadeDeleteService.deletePropertyGraph(property);
        propertyRepository.delete(property);
    }

    private static void apply(Property property, PropertyRequest request) {
        property.setName(request.name().trim());
        property.setCity(Strings.blankToNull(request.city()));
        property.setState(request.state() == null ? null : request.state().trim().toUpperCase(Locale.ROOT));
        property.setTotalAreaHa(request.totalAreaHa());
        property.setDescription(Strings.blankToNull(request.description()));
    }
}
