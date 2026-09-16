package com.shopflow.storefront;

import com.shopflow.category.Category;
import com.shopflow.category.CategoryRepository;
import com.shopflow.category.dto.CategoryDto;
import com.shopflow.exception.ResourceNotFoundException;
import com.shopflow.product.Product;
import com.shopflow.product.ProductRepository;
import com.shopflow.product.ProductService;
import com.shopflow.product.dto.ProductDto;
import com.shopflow.store.Store;
import com.shopflow.store.StoreRepository;
import com.shopflow.store.dto.StoreDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorefrontService {

    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public StorefrontService(StoreRepository storeRepository, CategoryRepository categoryRepository,
                             ProductRepository productRepository, ProductService productService) {
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public StoreDto getPublishedStore(String slug) {
        Store store = storeRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "slug", slug));
        return StoreDto.fromEntity(store);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getPublicCategories(String slug) {
        Store store = storeRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "slug", slug));
        return categoryRepository.findByStoreIdAndIsActiveTrueOrderByDisplayOrder(store.getId()).stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getPublicProducts(String slug, java.util.UUID categoryId, Pageable pageable) {
        Store store = storeRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "slug", slug));
        Page<Product> products;
        if (categoryId != null) {
            products = productRepository.findByStoreIdAndCategoryId(store.getId(), categoryId, pageable);
        } else {
            products = productRepository.findByStoreIdAndIsActiveTrue(store.getId(), pageable);
        }
        return products.map(productService::toDto);
    }
}
