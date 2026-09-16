package com.shopflow.inventory;

import com.shopflow.common.ApiResponse;
import com.shopflow.common.PagedResponse;
import com.shopflow.inventory.dto.LedgerEntryDto;
import com.shopflow.inventory.dto.StockAdjustmentRequest;
import com.shopflow.inventory.dto.StockMovementRequest;
import com.shopflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/inventory")
@Tag(name = "Inventory", description = "Inventory management")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/stock-in")
    @Operation(summary = "Stock in — add inventory")
    public ResponseEntity<ApiResponse<LedgerEntryDto>> stockIn(
            @PathVariable UUID storeId, @Valid @RequestBody StockMovementRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        LedgerEntryDto entry = inventoryService.stockIn(storeId, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(entry, "Stock added"));
    }

    @PostMapping("/stock-out")
    @Operation(summary = "Stock out — remove inventory")
    public ResponseEntity<ApiResponse<LedgerEntryDto>> stockOut(
            @PathVariable UUID storeId, @Valid @RequestBody StockMovementRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        LedgerEntryDto entry = inventoryService.stockOut(storeId, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(entry, "Stock removed"));
    }

    @PostMapping("/adjust")
    @Operation(summary = "Adjust stock — set exact quantity")
    public ResponseEntity<ApiResponse<LedgerEntryDto>> adjust(
            @PathVariable UUID storeId, @Valid @RequestBody StockAdjustmentRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        LedgerEntryDto entry = inventoryService.adjustStock(storeId, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(entry, "Stock adjusted"));
    }

    @GetMapping("/history")
    @Operation(summary = "Get inventory movement history")
    public ResponseEntity<PagedResponse<LedgerEntryDto>> history(
            @PathVariable UUID storeId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        Page<LedgerEntryDto> entries = inventoryService.getHistory(storeId, type, merchantId, PageRequest.of(page, size));
        return ResponseEntity.ok(PagedResponse.from(entries));
    }
}
