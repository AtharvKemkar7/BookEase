package com.bookease.provider;

import com.bookease.category.Category;
import com.bookease.category.CategoryRepository;
import com.bookease.common.exception.BusinessException;
import com.bookease.common.exception.ErrorCode;
import com.bookease.service.ProviderServiceMapper;
import com.bookease.service.ProviderServiceRepository;
import com.bookease.service.ProviderServiceResponse;
import com.bookease.user.User;
import com.bookease.user.UserRepository;
import com.bookease.user.UserRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProviderManagementService {

    private final ProviderRepository providerRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ProviderManagementService(
            ProviderRepository providerRepository,
            ProviderServiceRepository providerServiceRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.providerRepository = providerRepository;
        this.providerServiceRepository = providerServiceRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProviderResponse onboard(Long userId, ProviderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> notFound("User not found"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, "Administrators cannot become providers");
        }
        if (providerRepository.existsByUserId(userId)) {
            throw new BusinessException(
                    ErrorCode.PROVIDER_ALREADY_EXISTS, HttpStatus.CONFLICT, "A provider profile already exists");
        }
        Category category = requireCategory(request.categoryId());

        Provider provider = new Provider();
        provider.setUser(user);
        provider.setCategory(category);
        apply(provider, request.businessName(), request.description(),
                request.address(), request.city(), request.phone());
        provider.setStatus(ProviderStatus.PENDING);
        return ProviderMapper.toResponse(providerRepository.saveAndFlush(provider));
    }

    @Transactional(readOnly = true)
    public ProviderResponse getMine(Long userId) {
        return ProviderMapper.toResponse(requireByUser(userId));
    }

    @Transactional
    public ProviderResponse updateMine(Long userId, ProviderUpdateRequest request) {
        Provider provider = requireByUser(userId);
        Category category = requireCategory(request.categoryId());
        provider.setCategory(category);
        apply(provider, request.businessName(), request.description(),
                request.address(), request.city(), request.phone());
        if (provider.getStatus() == ProviderStatus.REJECTED) {
            provider.setStatus(ProviderStatus.PENDING);
        }
        return ProviderMapper.toResponse(providerRepository.saveAndFlush(provider));
    }

    @Transactional(readOnly = true)
    public Page<ProviderSummaryResponse> searchApproved(
            Long categoryId, String city, String name, Pageable pageable) {
        String normalizedCity = city == null || city.isBlank() ? null : city.trim().toLowerCase();
        String normalizedName = name == null || name.isBlank() ? null : name.trim().toLowerCase();
        return providerRepository.searchApproved(categoryId, normalizedCity, normalizedName, pageable)
                .map(ProviderMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ProviderSummaryResponse getApproved(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> notFound("Provider not found"));
        if (!provider.isApproved()) {
            throw notFound("Provider not found");
        }
        return ProviderMapper.toSummary(provider);
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> listApprovedServices(Long providerId) {
        requireApproved(providerId);
        return providerServiceRepository.findByProviderIdAndActiveTrueOrderByNameAsc(providerId).stream()
                .map(ProviderServiceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> pendingProviders() {
        return providerRepository.findByStatusOrderByCreatedAtAsc(ProviderStatus.PENDING).stream()
                .map(ProviderMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProviderResponse approve(Long providerId) {
        Provider provider = require(providerId);
        if (provider.getStatus() == ProviderStatus.APPROVED) {
            throw invalidState("Provider is already approved");
        }
        provider.setStatus(ProviderStatus.APPROVED);
        User owner = provider.getUser();
        if (owner.getRole() != UserRole.ADMIN) {
            owner.setRole(UserRole.PROVIDER);
            userRepository.save(owner);
        }
        return ProviderMapper.toResponse(providerRepository.saveAndFlush(provider));
    }

    @Transactional
    public ProviderResponse reject(Long providerId) {
        Provider provider = require(providerId);
        if (provider.getStatus() != ProviderStatus.PENDING) {
            throw invalidState("Only pending providers can be rejected");
        }
        provider.setStatus(ProviderStatus.REJECTED);
        return ProviderMapper.toResponse(providerRepository.saveAndFlush(provider));
    }

    @Transactional(readOnly = true)
    public Provider requireByUser(Long userId) {
        return providerRepository.findByUserId(userId)
                .orElseThrow(() -> notFound("Provider profile not found"));
    }

    @Transactional(readOnly = true)
    public Provider requireApproved(Long providerId) {
        Provider provider = require(providerId);
        if (!provider.isApproved()) {
            throw new BusinessException(
                    ErrorCode.PROVIDER_NOT_AVAILABLE,
                    HttpStatus.CONFLICT,
                    "Provider is not available for booking");
        }
        return provider;
    }

    private Provider require(Long providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> notFound("Provider not found"));
    }

    private Category requireCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> notFound("Category not found"));
        if (!category.isActive()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Category not found");
        }
        return category;
    }

    private static void apply(
            Provider provider, String businessName, String description,
            String address, String city, String phone) {
        provider.setBusinessName(businessName.trim());
        provider.setDescription(trimToNull(description));
        provider.setAddress(trimToNull(address));
        provider.setCity(trimToNull(city));
        provider.setPhone(trimToNull(phone));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    private static BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.INVALID_PROVIDER_STATE, HttpStatus.CONFLICT, message);
    }
}
