package com.shopflow.sales;

import com.shopflow.common.ApiResponse;
import com.shopflow.common.PagedResponse;
import com.shopflow.sales.dto.CreateSaleRequest;
import com.shopflow.sales.dto.SaleDto;
import com.shopflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/sales")
@Tag(name = "Sales", description = "Sales/billing management")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @PostMapping
    @Operation(summary = "Create a new sale")
    public ResponseEntity<ApiResponse<SaleDto>> create(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateSaleRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        SaleDto sale = salesService.createSale(storeId, request, idempotencyKey, merchantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(sale, "Sale recorded"));
    }

    @GetMapping
    @Operation(summary = "List sales")
    public ResponseEntity<PagedResponse<SaleDto>> list(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        Page<SaleDto> sales = salesService.getSales(storeId, merchantId, PageRequest.of(page, size));
        return ResponseEntity.ok(PagedResponse.from(sales));
    }

    @GetMapping("/{saleId}")
    @Operation(summary = "Get sale details")
    public ResponseEntity<ApiResponse<SaleDto>> get(
            @PathVariable UUID storeId, @PathVariable UUID saleId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        SaleDto sale = salesService.getSale(storeId, saleId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(sale));
    }
}
