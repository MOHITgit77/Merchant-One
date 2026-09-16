package com.shopflow.category;

import com.shopflow.category.dto.*;
import com.shopflow.exception.DuplicateResourceException;
import com.shopflow.exception.ResourceNotFoundException;
import com.shopflow.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final StoreService storeService;

    public CategoryService(CategoryRepository categoryRepository, StoreService storeService) {
        this.categoryRepository = categoryRepository;
        this.storeService = storeService;
    }

    @Transactional
    public CategoryDto createCategory(UUID storeId, CreateCategoryRequest request, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);

        if (categoryRepository.existsByStoreIdAndName(storeId, request.getName().trim())) {
            throw new DuplicateResourceException("A category with this name already exists in this store");
        }

        Integer maxOrder = categoryRepository.findMaxDisplayOrder(storeId);
        int nextOrder = (maxOrder != null) ? maxOrder + 1 : 0;

        Category category = new Category();
        category.setStoreId(storeId);
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(nextOrder);

        category = categoryRepository.save(category);
        log.info("Category created: {} in store {}", category.getName(), storeId);
        return CategoryDto.fromEntity(category);
    }

    @Transactional
    public CategoryDto updateCategory(UUID storeId, UUID categoryId, UpdateCategoryRequest request, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Category category = getCategoryForStore(categoryId, storeId);

        // Check for duplicate name (if name changed)
        if (!category.getName().equals(request.getName().trim()) &&
                categoryRepository.existsByStoreIdAndName(storeId, request.getName().trim())) {
            throw new DuplicateResourceException("A category with this name already exists in this store");
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category = categoryRepository.save(category);
        return CategoryDto.fromEntity(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(UUID storeId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        return categoryRepository.findByStoreIdOrderByDisplayOrder(storeId).stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategory(UUID storeId, UUID categoryId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Category category = getCategoryForStore(categoryId, storeId);
        return CategoryDto.fromEntity(category);
    }

    @Transactional
    public CategoryDto activateCategory(UUID storeId, UUID categoryId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Category category = getCategoryForStore(categoryId, storeId);
        category.setActive(true);
        category = categoryRepository.save(category);
        log.info("Category activated: {} in store {}", category.getName(), storeId);
        return CategoryDto.fromEntity(category);
    }

    @Transactional
    public CategoryDto deactivateCategory(UUID storeId, UUID categoryId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Category category = getCategoryForStore(categoryId, storeId);
        category.setActive(false);
        // Products remain assigned — they just won't show on storefront
        category = categoryRepository.save(category);
        log.info("Category deactivated: {} in store {}", category.getName(), storeId);
        return CategoryDto.fromEntity(category);
    }

    @Transactional
    public List<CategoryDto> reorderCategories(UUID storeId, ReorderRequest request, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);

        List<UUID> orderedIds = request.getCategoryIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            Category category = getCategoryForStore(orderedIds.get(i), storeId);
            category.setDisplayOrder(i);
            categoryRepository.save(category);
        }

        return getCategories(storeId, merchantId);
    }

    private Category getCategoryForStore(UUID categoryId, UUID storeId) {
        return categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
    }
}
