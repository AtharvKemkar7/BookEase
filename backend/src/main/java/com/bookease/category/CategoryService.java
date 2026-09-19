package com.bookease.category;

import com.bookease.common.exception.BusinessException;
import com.bookease.common.exception.ErrorCode;
import com.bookease.common.validation.NameNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = NameNormalizer.display(request.name());
        String key = NameNormalizer.key(request.name());
        if (categoryRepository.existsByNameKey(key)) {
            throw duplicateCategory();
        }

        Category category = new Category();
        category.setName(name);
        category.setNameKey(key);
        category.setDescription(trimToNull(request.description()));
        category.setActive(request.active() == null || request.active());
        try {
            return CategoryMapper.toResponse(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw duplicateCategory();
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return CategoryMapper.toResponse(require(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = require(id);
        String key = NameNormalizer.key(request.name());
        if (categoryRepository.existsByNameKeyAndIdNot(key, id)) {
            throw duplicateCategory();
        }
        category.setName(NameNormalizer.display(request.name()));
        category.setNameKey(key);
        category.setDescription(trimToNull(request.description()));
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryMapper.toResponse(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    public void deactivate(Long id) {
        Category category = require(id);
        category.setActive(false);
        categoryRepository.saveAndFlush(category);
    }

    private Category require(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Category not found"));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BusinessException duplicateCategory() {
        return new BusinessException(
                ErrorCode.DUPLICATE_CATEGORY, HttpStatus.CONFLICT, "A category with this name already exists");
    }
}
