package com.shopflow.product;

import com.shopflow.category.Category;
import com.shopflow.category.CategoryRepository;
import com.shopflow.exception.DuplicateResourceException;
import com.shopflow.exception.ResourceNotFoundException;
import com.shopflow.product.dto.CreateProductRequest;
import com.shopflow.product.dto.ProductDto;
import com.shopflow.store.FileStorageService;
import com.shopflow.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final StoreService storeService;
    private final FileStorageService fileStorageService;

    public ProductService(ProductRepository productRepository, ProductVariantRepository variantRepository,
                          ProductImageRepository imageRepository, CategoryRepository categoryRepository,
                          StoreService storeService, FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.categoryRepository = categoryRepository;
        this.storeService = storeService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public ProductDto createProduct(UUID storeId, CreateProductRequest request, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);

        if (request.getCategoryId() != null) {
            categoryRepository.findByIdAndStoreId(request.getCategoryId(), storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        Product product = new Product();
        product.setStoreId(storeId);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategoryId(request.getCategoryId());
        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setCostPrice(request.getCostPrice());
        product.setTaxPercent(request.getTaxPercent());
        product.setUnit(request.getUnit() != null ? request.getUnit() : ProductUnit.PCS);
        product.setTrackInventory(request.isTrackInventory());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setHasVariants(request.isHasVariants());

        product = productRepository.save(product);

        if (request.isHasVariants() && request.getVariants() != null && !request.getVariants().isEmpty()) {
            AtomicInteger order = new AtomicInteger(0);
            for (CreateProductRequest.VariantRequest vr : request.getVariants()) {
                createVariant(product, vr, order.getAndIncrement());
            }
        } else {
            // Create default variant for non-variant products
            ProductVariant defaultVariant = new ProductVariant();
            defaultVariant.setProduct(product);
            defaultVariant.setName("Default");
            defaultVariant.setSku(generateSku(product.getName()));
            defaultVariant.setPrice(product.getPrice());
            defaultVariant.setCostPrice(product.getCostPrice());
            defaultVariant.setDisplayOrder(0);
            product.getVariants().add(defaultVariant);
            variantRepository.save(defaultVariant);
        }

        log.info("Product created: {} in store {}", product.getName(), storeId);
        return toDto(product);
    }

    @Transactional
    public ProductDto updateProduct(UUID storeId, UUID productId, CreateProductRequest request, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Product product = getProductForStore(productId, storeId);

        if (request.getCategoryId() != null) {
            categoryRepository.findByIdAndStoreId(request.getCategoryId(), storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategoryId(request.getCategoryId());
        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setCostPrice(request.getCostPrice());
        product.setTaxPercent(request.getTaxPercent());
        product.setUnit(request.getUnit() != null ? request.getUnit() : product.getUnit());
        product.setTrackInventory(request.isTrackInventory());
        product.setLowStockThreshold(request.getLowStockThreshold());

        productRepository.save(product);
        return toDto(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(UUID storeId, UUID categoryId, String search, UUID merchantId, Pageable pageable) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Page<Product> products;
        if (search != null && !search.isBlank()) {
            products = productRepository.findByStoreIdAndNameContainingIgnoreCase(storeId, search.trim(), pageable);
        } else if (categoryId != null) {
            products = productRepository.findByStoreIdAndCategoryId(storeId, categoryId, pageable);
        } else {
            products = productRepository.findByStoreId(storeId, pageable);
        }
        return products.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID storeId, UUID productId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Product product = getProductForStore(productId, storeId);
        return toDto(product);
    }

    @Transactional
    public ProductDto activateProduct(UUID storeId, UUID productId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Product product = getProductForStore(productId, storeId);
        product.setActive(true);
        productRepository.save(product);
        return toDto(product);
    }

    @Transactional
    public ProductDto deactivateProduct(UUID storeId, UUID productId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Product product = getProductForStore(productId, storeId);
        product.setActive(false);
        productRepository.save(product);
        return toDto(product);
    }

    @Transactional
    public ProductDto duplicateProduct(UUID storeId, UUID productId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Product source = getProductForStore(productId, storeId);

        Product copy = new Product();
        copy.setStoreId(storeId);
        copy.setName(source.getName() + " (Copy)");
        copy.setDescription(source.getDescription());
        copy.setCategoryId(source.getCategoryId());
        copy.setPrice(source.getPrice());
        copy.setCompareAtPrice(source.getCompareAtPrice());
        copy.setCostPrice(source.getCostPrice());
        copy.setTaxPercent(source.getTaxPercent());
        copy.setUnit(source.getUnit());
        copy.setTrackInventory(source.isTrackInventory());
        copy.setLowStockThreshold(source.getLowStockThreshold());
        copy.setHasVariants(source.isHasVariants());
        copy.setActive(false);
        copy = productRepository.save(copy);

        for (ProductVariant sv : source.getVariants()) {
            ProductVariant cv = new ProductVariant();
            cv.setProduct(copy);
            cv.setName(sv.getName());
            cv.setSku(generateSku(copy.getName() + "-" + sv.getName()));
            cv.setPrice(sv.getPrice());
            cv.setCostPrice(sv.getCostPrice());
            cv.setDisplayOrder(sv.getDisplayOrder());
            cv.setBarcode(null);
            variantRepository.save(cv);
        }

        return toDto(copy);
    }

    @Transactional
    public ProductDto uploadImage(UUID storeId, UUID productId, MultipartFile file, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Product product = getProductForStore(productId, storeId);

        String url = fileStorageService.storeFile(file, "stores/" + storeId + "/products/" + productId);
        int order = imageRepository.countByProductId(productId);

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(url);
        image.setDisplayOrder(order);
        imageRepository.save(image);

        return toDto(productRepository.findById(productId).orElseThrow());
    }

    @Transactional
    public void deleteImage(UUID storeId, UUID productId, UUID imageId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        getProductForStore(productId, storeId);
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));
        imageRepository.delete(image);
    }

    // ========== Helpers ==========

    Product getProductForStore(UUID productId, UUID storeId) {
        return productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    private void createVariant(Product product, CreateProductRequest.VariantRequest vr, int order) {
        String sku = vr.getSku() != null && !vr.getSku().isBlank() ? vr.getSku().trim() : generateSku(product.getName() + "-" + vr.getName());
        if (variantRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("SKU '" + sku + "' already exists");
        }
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName(vr.getName().trim());
        variant.setSku(sku);
        variant.setPrice(vr.getPrice());
        variant.setCostPrice(vr.getCostPrice());
        variant.setBarcode(vr.getBarcode());
        variant.setWeight(vr.getWeight());
        variant.setDisplayOrder(order);
        variantRepository.save(variant);
    }

    String generateSku(String base) {
        String clean = base.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (clean.length() > 8) clean = clean.substring(0, 8);
        return "SKU-" + clean + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setStoreId(product.getStoreId());
        dto.setCategoryId(product.getCategoryId());
        if (product.getCategoryId() != null) {
            categoryRepository.findById(product.getCategoryId()).ifPresent(c -> dto.setCategoryName(c.getName()));
        }
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCompareAtPrice(product.getCompareAtPrice());
        dto.setCostPrice(product.getCostPrice());
        dto.setTaxPercent(product.getTaxPercent());
        dto.setUnit(product.getUnit());
        dto.setActive(product.isActive());
        dto.setHasVariants(product.isHasVariants());
        dto.setTrackInventory(product.isTrackInventory());
        dto.setLowStockThreshold(product.getLowStockThreshold());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        List<ProductDto.VariantDto> variantDtos = product.getVariants().stream().map(v -> {
            ProductDto.VariantDto vd = new ProductDto.VariantDto();
            vd.setId(v.getId());
            vd.setName(v.getName());
            vd.setSku(v.getSku());
            vd.setPrice(v.getPrice());
            vd.setCostPrice(v.getCostPrice());
            vd.setQuantityOnHand(v.getQuantityOnHand());
            vd.setReservedQuantity(v.getReservedQuantity());
            vd.setAvailableQuantity(v.getAvailableQuantity());
            vd.setActive(v.isActive());
            vd.setBarcode(v.getBarcode());
            vd.setWeight(v.getWeight());
            return vd;
        }).collect(Collectors.toList());
        dto.setVariants(variantDtos);
        dto.setTotalStock(variantDtos.stream().mapToInt(ProductDto.VariantDto::getQuantityOnHand).sum());

        List<ProductDto.ImageDto> imageDtos = product.getImages().stream().map(i -> {
            ProductDto.ImageDto id = new ProductDto.ImageDto();
            id.setId(i.getId());
            id.setImageUrl(i.getImageUrl());
            id.setDisplayOrder(i.getDisplayOrder());
            id.setAltText(i.getAltText());
            return id;
        }).collect(Collectors.toList());
        dto.setImages(imageDtos);

        return dto;
    }
}
