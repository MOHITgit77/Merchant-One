package com.shopflow.purchase;

import com.shopflow.common.ApiResponse;
import com.shopflow.common.PagedResponse;
import com.shopflow.purchase.dto.BatchDto;
import com.shopflow.purchase.dto.CreatePurchaseRequest;
import com.shopflow.purchase.dto.PurchaseDto;
import com.shopflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}")
@Tag(name = "Purchases & Batches", description = "Purchase orders and batch management")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/purchases")
    @Operation(summary = "Create a purchase order")
    public ResponseEntity<ApiResponse<PurchaseDto>> create(
            @PathVariable UUID storeId, @Valid @RequestBody CreatePurchaseRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        PurchaseDto purchase = purchaseService.createPurchase(storeId, request, merchantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(purchase, "Purchase recorded"));
    }

    @GetMapping("/purchases")
    @Operation(summary = "List purchases")
    public ResponseEntity<PagedResponse<PurchaseDto>> list(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        Page<PurchaseDto> purchases = purchaseService.getPurchases(storeId, merchantId, PageRequest.of(page, size));
        return ResponseEntity.ok(PagedResponse.from(purchases));
    }

    @GetMapping("/purchases/{purchaseId}")
    @Operation(summary = "Get purchase details")
    public ResponseEntity<ApiResponse<PurchaseDto>> get(
            @PathVariable UUID storeId, @PathVariable UUID purchaseId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        PurchaseDto purchase = purchaseService.getPurchase(storeId, purchaseId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(purchase));
    }

    @GetMapping("/batches")
    @Operation(summary = "List batches")
    public ResponseEntity<PagedResponse<BatchDto>> batches(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        Page<BatchDto> batches = purchaseService.getBatches(storeId, merchantId, PageRequest.of(page, size));
        return ResponseEntity.ok(PagedResponse.from(batches));
    }

    @GetMapping("/batches/expiring")
    @Operation(summary = "Get expiring batches (within 30 days)")
    public ResponseEntity<ApiResponse<List<BatchDto>>> expiringBatches(@PathVariable UUID storeId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        List<BatchDto> batches = purchaseService.getExpiringBatches(storeId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(batches));
    }
}
