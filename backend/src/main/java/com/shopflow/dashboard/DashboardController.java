package com.shopflow.dashboard;

import com.shopflow.common.ApiResponse;
import com.shopflow.product.ProductRepository;
import com.shopflow.product.ProductVariantRepository;
import com.shopflow.sales.SaleRepository;
import com.shopflow.security.SecurityUtils;
import com.shopflow.store.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/dashboard")
@Tag(name = "Dashboard", description = "Dashboard metrics")
public class DashboardController {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final StoreService storeService;

    public DashboardController(SaleRepository saleRepository, ProductRepository productRepository,
                                ProductVariantRepository variantRepository, StoreService storeService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.storeService = storeService;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get dashboard metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetrics(@PathVariable UUID storeId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        storeService.verifyStoreOwnership(storeId, merchantId);

        Instant todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("todaySales", saleRepository.countSalesSince(storeId, todayStart));
        metrics.put("todayRevenue", saleRepository.totalRevenueSince(storeId, todayStart));
        metrics.put("monthSales", saleRepository.countSalesSince(storeId, monthStart));
        metrics.put("monthRevenue", saleRepository.totalRevenueSince(storeId, monthStart));
        metrics.put("totalProducts", productRepository.countByStoreId(storeId));
        metrics.put("lowStockCount", variantRepository.findLowStockVariants(storeId, 5).size());

        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}
