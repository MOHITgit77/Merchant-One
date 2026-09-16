package com.shopflow.storefront;

import com.shopflow.category.dto.CategoryDto;
import com.shopflow.common.ApiResponse;
import com.shopflow.common.PagedResponse;
import com.shopflow.product.dto.ProductDto;
import com.shopflow.store.dto.StoreDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/storefront")
@Tag(name = "Storefront", description = "Public storefront API")
public class StorefrontController {

    private final StorefrontService storefrontService;

    public StorefrontController(StorefrontService storefrontService) {
        this.storefrontService = storefrontService;
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get published store by slug")
    public ResponseEntity<ApiResponse<StoreDto>> getStore(@PathVariable String slug) {
        StoreDto store = storefrontService.getPublishedStore(slug);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @GetMapping("/{slug}/categories")
    @Operation(summary = "Get categories for a published store")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories(@PathVariable String slug) {
        List<CategoryDto> categories = storefrontService.getPublicCategories(slug);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{slug}/products")
    @Operation(summary = "Get products for a published store")
    public ResponseEntity<PagedResponse<ProductDto>> getProducts(
            @PathVariable String slug,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductDto> products = storefrontService.getPublicProducts(slug, categoryId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PagedResponse.from(products));
    }
}
