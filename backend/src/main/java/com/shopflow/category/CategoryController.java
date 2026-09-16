package com.shopflow.category;

import com.shopflow.category.dto.*;
import com.shopflow.common.ApiResponse;
import com.shopflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/categories")
@Tag(name = "Categories", description = "Category management")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @Operation(summary = "Create a category")
    public ResponseEntity<ApiResponse<CategoryDto>> create(
            @PathVariable UUID storeId, @Valid @RequestBody CreateCategoryRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        CategoryDto category = categoryService.createCategory(storeId, request, merchantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(category, "Category created"));
    }

    @GetMapping
    @Operation(summary = "List categories for a store")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> list(@PathVariable UUID storeId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        List<CategoryDto> categories = categoryService.getCategories(storeId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get a category by ID")
    public ResponseEntity<ApiResponse<CategoryDto>> get(
            @PathVariable UUID storeId, @PathVariable UUID categoryId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        CategoryDto category = categoryService.getCategory(storeId, categoryId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<CategoryDto>> update(
            @PathVariable UUID storeId, @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        CategoryDto category = categoryService.updateCategory(storeId, categoryId, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated"));
    }

    @PatchMapping("/{categoryId}/activate")
    @Operation(summary = "Activate a category")
    public ResponseEntity<ApiResponse<CategoryDto>> activate(
            @PathVariable UUID storeId, @PathVariable UUID categoryId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        CategoryDto category = categoryService.activateCategory(storeId, categoryId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(category, "Category activated"));
    }

    @PatchMapping("/{categoryId}/deactivate")
    @Operation(summary = "Deactivate a category")
    public ResponseEntity<ApiResponse<CategoryDto>> deactivate(
            @PathVariable UUID storeId, @PathVariable UUID categoryId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        CategoryDto category = categoryService.deactivateCategory(storeId, categoryId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(category, "Category deactivated"));
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> reorder(
            @PathVariable UUID storeId, @RequestBody ReorderRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        List<CategoryDto> categories = categoryService.reorderCategories(storeId, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories reordered"));
    }
}
