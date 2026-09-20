package com.bookease.provider;

import com.bookease.common.exception.BusinessException;
import com.bookease.common.exception.ErrorCode;
import com.bookease.common.validation.NameNormalizer;
import com.bookease.service.ProviderService;
import com.bookease.service.ProviderServiceMapper;
import com.bookease.service.ProviderServiceRepository;
import com.bookease.service.ProviderServiceRequest;
import com.bookease.service.ProviderServiceResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProviderOfferingService {

    private final ProviderServiceRepository providerServiceRepository;
    private final ProviderManagementService providerManagementService;

    public ProviderOfferingService(
            ProviderServiceRepository providerServiceRepository,
            ProviderManagementService providerManagementService) {
        this.providerServiceRepository = providerServiceRepository;
        this.providerManagementService = providerManagementService;
    }

    @Transactional
    public ProviderServiceResponse create(Long userId, ProviderServiceRequest request) {
        Provider provider = providerManagementService.requireByUser(userId);
        String key = NameNormalizer.key(request.name());
        if (providerServiceRepository.existsByProviderIdAndNameKeyAndActiveTrue(provider.getId(), key)) {
            throw duplicateService();
        }

        ProviderService service = new ProviderService();
        service.setProvider(provider);
        apply(service, request);
        try {
            return ProviderServiceMapper.toResponse(providerServiceRepository.saveAndFlush(service));
        } catch (DataIntegrityViolationException ex) {
            throw duplicateService();
        }
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> listMine(Long userId) {
        Provider provider = providerManagementService.requireByUser(userId);
        return providerServiceRepository.findByProviderIdOrderByNameAsc(provider.getId()).stream()
                .map(ProviderServiceMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProviderServiceResponse update(Long userId, Long serviceId, ProviderServiceRequest request) {
        Provider provider = providerManagementService.requireByUser(userId);
        ProviderService service = requireOwned(provider.getId(), serviceId);
        String key = NameNormalizer.key(request.name());
        if (providerServiceRepository.existsByProviderIdAndNameKeyAndActiveTrueAndIdNot(
                provider.getId(), key, serviceId)) {
            throw duplicateService();
        }
        apply(service, request);
        return ProviderServiceMapper.toResponse(providerServiceRepository.saveAndFlush(service));
    }

    @Transactional
    public void deactivate(Long userId, Long serviceId) {
        Provider provider = providerManagementService.requireByUser(userId);
        ProviderService service = requireOwned(provider.getId(), serviceId);
        service.setActive(false);
        providerServiceRepository.saveAndFlush(service);
    }

    private ProviderService requireOwned(Long providerId, Long serviceId) {
        return providerServiceRepository.findByIdAndProviderId(serviceId, providerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Service not found"));
    }

    private static void apply(ProviderService service, ProviderServiceRequest request) {
        service.setName(NameNormalizer.display(request.name()));
        service.setNameKey(NameNormalizer.key(request.name()));
        service.setDescription(trimToNull(request.description()));
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        if (request.active() != null) {
            service.setActive(request.active());
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BusinessException duplicateService() {
        return new BusinessException(
                ErrorCode.DUPLICATE_SERVICE,
                HttpStatus.CONFLICT,
                "An active service with this name already exists");
    }
}
