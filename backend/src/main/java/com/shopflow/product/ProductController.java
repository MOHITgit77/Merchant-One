package com.shopflow.product;

import com.shopflow.common.ApiResponse;
import com.shopflow.common.PagedResponse;
import com.shopflow.product.dto.CreateProductRequest;
import com.shopflow.product.dto.ProductDto;
import com.shopflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/products")
@Tag(name = "Products", description = "Product catalogue management")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create a product")
    public ResponseEntity<ApiResponse<ProductDto>> create(
            @PathVariable UUID storeId, @Valid @RequestBody CreateProductRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        ProductDto product = productService.createProduct(storeId, request, merchantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(product, "Product created"));
    }

    @GetMapping
    @Operation(summary = "List products")
    public ResponseEntity<PagedResponse<ProductDto>> list(
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        Page<ProductDto> products = productService.getProducts(storeId, categoryId, search, merchantId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PagedResponse.from(products));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductDto>> get(
            @PathVariable UUID storeId, @PathVariable UUID productId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        ProductDto product = productService.getProduct(storeId, productId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update product")
    public ResponseEntity<ApiResponse<ProductDto>> update(
            @PathVariable UUID storeId, @PathVariable UUID productId,
            @Valid @RequestBody CreateProductRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        ProductDto product = productService.updateProduct(storeId, productId, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated"));
    }

    @PatchMapping("/{productId}/activate")
    @Operation(summary = "Activate product")
    public ResponseEntity<ApiResponse<ProductDto>> activate(
            @PathVariable UUID storeId, @PathVariable UUID productId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(productService.activateProduct(storeId, productId, merchantId), "Product activated"));
    }

    @PatchMapping("/{productId}/deactivate")
    @Operation(summary = "Deactivate product")
    public ResponseEntity<ApiResponse<ProductDto>> deactivate(
            @PathVariable UUID storeId, @PathVariable UUID productId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(productService.deactivateProduct(storeId, productId, merchantId), "Product deactivated"));
    }

    @PostMapping("/{productId}/duplicate")
    @Operation(summary = "Duplicate product")
    public ResponseEntity<ApiResponse<ProductDto>> duplicate(
            @PathVariable UUID storeId, @PathVariable UUID productId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        ProductDto product = productService.duplicateProduct(storeId, productId, merchantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(product, "Product duplicated"));
    }

    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product image")
    public ResponseEntity<ApiResponse<ProductDto>> uploadImage(
            @PathVariable UUID storeId, @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        ProductDto product = productService.uploadImage(storeId, productId, file, merchantId);
        return ResponseEntity.ok(ApiResponse.success(product, "Image uploaded"));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @Operation(summary = "Delete product image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID storeId, @PathVariable UUID productId, @PathVariable UUID imageId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        productService.deleteImage(storeId, productId, imageId, merchantId);
        return ResponseEntity.ok(ApiResponse.message("Image deleted"));
    }
}
