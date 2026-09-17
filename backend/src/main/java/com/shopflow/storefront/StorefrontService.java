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
import com.shopflow.store.DeliverySettingsRepository;
import com.shopflow.store.PaymentPreferenceRepository;
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
    private final com.shopflow.order.OrderService orderService;

    private final DeliverySettingsRepository deliverySettingsRepository;
    private final PaymentPreferenceRepository paymentPreferenceRepository;

    public StorefrontService(StoreRepository storeRepository,
                             CategoryRepository categoryRepository,
                             ProductRepository productRepository,
                             ProductService productService,
                             com.shopflow.order.OrderService orderService,
                             DeliverySettingsRepository deliverySettingsRepository,
                             PaymentPreferenceRepository paymentPreferenceRepository) {
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.orderService = orderService;
        this.deliverySettingsRepository = deliverySettingsRepository;
        this.paymentPreferenceRepository = paymentPreferenceRepository;
    }

    @Transactional(readOnly = true)
    public com.shopflow.storefront.dto.StorefrontStoreDto getPublishedStore(String slug) {
        Store store = storeRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "slug", slug));
        
        com.shopflow.storefront.dto.StorefrontStoreDto dto = new com.shopflow.storefront.dto.StorefrontStoreDto();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setSlug(store.getSlug());
        dto.setDescription(store.getDescription());
        dto.setLogoUrl(store.getLogoUrl());
        dto.setCoverImageUrl(store.getCoverImageUrl());
        
        dto.setPhone(store.getPhone());
        dto.setEmail(store.getEmail());
        dto.setAddress(store.getAddress());

        dto.setPickupEnabled(store.isPickupEnabled());
        
        deliverySettingsRepository.findByStoreId(store.getId()).ifPresent(ds -> {
            dto.setDeliveryEnabled(ds.isEnabled());
            dto.setDeliveryRadius(ds.getDeliveryRadius());
            dto.setDeliveryFee(ds.getDeliveryFee());
            dto.setMinimumOrder(ds.getMinimumOrder());
            dto.setFreeDeliveryThreshold(ds.getFreeDeliveryThreshold());
        });

        List<String> paymentMethods = paymentPreferenceRepository.findByStoreId(store.getId())
            .stream()
            .filter(com.shopflow.store.PaymentPreference::isEnabled)
            .map(p -> p.getPaymentMethod().name())
            .collect(Collectors.toList());
        dto.setPaymentMethods(paymentMethods);

        return dto;
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
            products = productRepository.findByStoreIdAndCategoryIdAndIsActiveTrue(store.getId(), categoryId, pageable);
        } else {
            products = productRepository.findByStoreIdAndIsActiveTrue(store.getId(), pageable);
        }
        return products.map(productService::toPublicDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getPublicProduct(String slug, java.util.UUID productId) {
        Store store = storeRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "slug", slug));
        Product product = productRepository.findByIdAndStoreIdAndIsActiveTrue(productId, store.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        return productService.toPublicDto(product);
    }

    @Transactional
    public com.shopflow.order.dto.OrderDto createCustomerOrder(String slug, com.shopflow.order.dto.CreateOrderRequest request) {
        Store store = storeRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "slug", slug));
        
        if (request.getOrderType() == com.shopflow.order.OrderType.PICKUP && !store.isPickupEnabled()) {
            throw new com.shopflow.exception.BusinessRuleException("DELIVERY_ONLY", "Store does not support pickup");
        }

        return orderService.createOrder(store.getId(), request);
    }

}

