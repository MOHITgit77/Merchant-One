package com.shopflow.order;

import com.shopflow.common.ApiResponse;
import com.shopflow.common.PagedResponse;
import com.shopflow.order.dto.*;
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
@RequestMapping("/api/stores/{storeId}/orders")
@Tag(name = "Merchant Orders", description = "Order management for merchants")
public class MerchantOrderController {

    private final OrderService orderService;

    public MerchantOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List all orders for a store")
    public ResponseEntity<PagedResponse<OrderDto>> getOrders(
            @PathVariable UUID storeId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        Page<OrderDto> orders = orderService.getOrders(storeId, status, merchantId, PageRequest.of(page, size));
        return ResponseEntity.ok(PagedResponse.from(orders));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get specific order details")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(
            @PathVariable UUID storeId, @PathVariable UUID orderId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        OrderDto order = orderService.getOrder(storeId, orderId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PatchMapping("/{orderId}/accept")
    @Operation(summary = "Accept an order (reserves stock)")
    public ResponseEntity<ApiResponse<OrderDto>> acceptOrder(
            @PathVariable UUID storeId, @PathVariable UUID orderId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        OrderDto order = orderService.acceptOrder(storeId, orderId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order accepted successfully"));
    }

    @PatchMapping("/{orderId}/reject")
    @Operation(summary = "Reject an order")
    public ResponseEntity<ApiResponse<OrderDto>> rejectOrder(
            @PathVariable UUID storeId, @PathVariable UUID orderId,
            @RequestBody(required = false) RejectOrderRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        OrderDto order = orderService.rejectOrder(storeId, orderId, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order rejected"));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status (e.g., to PREPARING or READY)")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable UUID storeId, @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        OrderDto order = orderService.updateOrderStatus(storeId, orderId, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated"));
    }

    @PatchMapping("/{orderId}/complete")
    @Operation(summary = "Complete an order (deducts stock, finalizes)")
    public ResponseEntity<ApiResponse<OrderDto>> completeOrder(
            @PathVariable UUID storeId, @PathVariable UUID orderId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        OrderDto order = orderService.completeOrder(storeId, orderId, merchantId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order completed successfully"));
    }

    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order (releases reserved stock)")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable UUID storeId, @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        OrderDto order = orderService.cancelOrder(storeId, orderId, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled"));
    }
}
